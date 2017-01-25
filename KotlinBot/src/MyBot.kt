import java.util.logging.Level
import java.util.logging.Logger


/**
 * Created by David on 1
 * 4/01/2017.
 */


/*
 * TODOs
 *
 *
 *  SIMPLE
 *   - prevent str loss to cap
 *   - tunnel to high prod places
 *
 *  INTERMEDIATE
 *   - move strength where it's needed
 *   - attacking through combining cells
 *   - avoid opening new lanes
 *
 *  LARGE, FUNDAMENTAL
 *   - move towards high prod, low str stuff
 *   - map analysis
 *	  - A* / Dijkstra
 *
 */

/* V2 STATUS

Things that work:
 - dijkstra's
 - tunneling to high prod places

Things that need work
 - *slow starts*
 - combat

Interesting details

 - overall: tweak heuristics for dijkstra's!!

 - expansion: combine cells to attack [might not be a net positive change]
 - expansion: decide when it's worth to tunnel and when we should expand

 - combat: avoid new lanes
 - combat: move to important locations
 - combat: avoid str loss to cap [low prio]



 */


/* Constants */
val PROD_MULTIPLIER = 4 // because of <=, I'm setting it at 1 less than required
val NEAREST_DIR_SEARCH_FACTOR = 1
val SEARCH_RADIUS = 7
val NEAREST_SEARCH_LEVEL = 12
val TIME_SOFT_CAP = 1300
val PROD_IMPORTANCE_FACTOR = 0.7f //.5 when it's weighed the same, 1.0 disregards STR

/* Logging */
val LOGGING_LEVEL: Level = Level.OFF // We can debug now!
const val BOT_NAME = "DavKotlinBot_v1"
const val LOGFILE_PREFIX = "../logs/"
const val LOGFILE_SUFFIX = ".txt"

/* Map Processing */

/*
 * should be a multiple of 5 because of conventional map dimensions
 */
val SECTORS_BY_SIDE = 10

/*
 * size of the edge of the blur applied
 */
val BLUR_DIMENSION = 3

/*
 * 0 desirability | 1 enemy army strength | 2 friendly power arriving
 */
val PROCESSING_PROPERTIES = 3


/* Globals */
var myId = 0
var turnCounter = 0
var gameMap: GameMap? = null
var state = GameState.INITIAL

/* Algorithm stuff */
var processedMap: Array<Array<FloatArray>>? = null
var qualityMap: Array<Array<FloatArray>>? = null


/* Pathfinding */
var graph = HaliteGraph()

var regionMap: Array<Array<FloatArray>>? = null

/* Logging */
var logger: Logger = initLog()
var turnTimes: MutableList<Int>? = null


fun main(args: Array<String>) {

    val startTime = System.currentTimeMillis()

    /* Initialization */

    val iPackage = Networking.getInit()
    myId = iPackage.myID
    gameMap = iPackage.map
    val gameMap = gameMap!!

    initMap()
    graph = HaliteGraph(GameMap.map)

    Networking.sendInit(BOT_NAME)
    logger.info("Initialization took ${System.currentTimeMillis() - startTime / 1000} seconds.")

    while (true) {

        turnCounter++
        val turnStart = System.currentTimeMillis()

        val moves = mutableListOf<Move>()
        Networking.updateFrame(gameMap)

        for (y in 0 until gameMap.height) {
            for (x in 0 until gameMap.width) {

                val location = gameMap.getLocation(x, y)
                val site = gameMap.getSite(location)


                if (site.owner == myId) {

                    /* Timeout protection of sorts */
                    val move = if ((System.currentTimeMillis() - turnStart) < TIME_SOFT_CAP)
                        Move(location, nextMove(location)) else Move(location, Direction.STILL)

                    moves.add(move)
                }
            }
        }
        logTurn(turnStart)
        Networking.sendFrame(moves)
    }
}

fun nextMove(loc: Location): Direction {
    val gameMap = gameMap!!
    val site = gameMap.getSite(loc)

    val location = detectBorder(loc)


    /* Don't move, ever, with 0 str */
    if (site.strength == 0) return Direction.STILL


    /* Main logic loop */
    when (state) {

    /* Early game, expanding
     * TODO figure out diff between initial/expanding
     */
        GameState.INITIAL,
        GameState.EXPANDING -> {
            logger.info { "Cell [${loc.x},${loc.y}]. Initial state, location: $location" }
            when (location) {
                TileLocation.INNER -> {
                    /* Early game, expansion, inner cell */

                    if (site.strength > PROD_MULTIPLIER * site.production) {
                        return nearestNeutralDirNaive(loc)
                    }
                    return Direction.STILL
                }

                TileLocation.BORDER -> {
                    /* Early game, expansion, on the border */

                    val dest = getBestLocation(loc) ?: Location(-1, -1)
                    if (dest.x == -1) {//TODO what happens when BestLocation finds nothing?
                        logger.severe { "$loc can't find where to go, going north." }
                        return Direction.NORTH
                    }


                    val path = graph.path(loc, dest)
                    val nextTile = path.vertexList[1]

                    logger.severe { "$turnCounter | Tile: [$loc]  Destination : $dest. Through: $nextTile" }


                    val nextSite = gameMap.getSite(nextTile)
                    if ((site.strength >= nextSite.strength && nextSite.owner != myId) || (nextSite.owner == myId && site.strength > (PROD_MULTIPLIER * site.production))) {
                        logger.severe { "$loc moving to $nextTile, checks passed woo" }
                        return loc.directionTo(nextTile)
                    }

                    return Direction.STILL

                }


                TileLocation.WARZONE -> {
                    /* Encountered an enemy for the first time! */
                    toggleState(GameState.COMBAT)

                    /* Go by overkill heuristic for now */
                    return loc.directionTo(loc.maxBy(::heuristic))

                }

            }


        }

    /* We're fighting players */
        GameState.COMBAT -> {
            logger.severe { "Cell [${loc.x},${loc.y}]. Combat state, location: $location" }
            when (location) {
                TileLocation.INNER -> {
                    /* Send reinforcements */
                    val nextTile = nearestEnemyDirNaive(loc)
                    if (site.strength > site.production * PROD_MULTIPLIER) {
                        return nextTile
                    }

                    return Direction.STILL
                }

                TileLocation.BORDER -> {
                    /* Send reinforcements or expand
                     * TODO figure out when to prioritize what
                     */

                    val dest = getBestLocation(loc) ?: Location(-1, -1)
                    if (dest.x == -1) return Direction.NORTH //TODO what happens when BestLocation finds nothing?

                    logger.severe { "$turnCounter | Tile: [${loc.x}, ${loc.y}]  Dest : [${dest.x},${dest.y}]" }

                    val path = graph.path(loc, dest)
                    val nextTile = path.vertexList[1]


                    logger.fine { ("Turn $turnCounter | Loc: [${loc.x},${loc.y}] path.first: ${nextTile.x},${nextTile.y}") }


                    if (site.strength >= gameMap.getSite(nextTile).strength)
                        return loc.directionTo(nextTile)


                }

                TileLocation.WARZONE -> {
                    /* FIGHT! */

                    /* Go by overkill heuristic for now */
                    return loc.directionTo(loc.maxBy(::heuristic))


                }
            }
        }
    }


    /* Fail-safe */
    logger.warning(String
            .format("Turn $turnCounter: [${loc.x}, ${loc.y}] Couldn't come up with a move. Standing still.",
                    turnCounter, loc.x, loc.y))
    return Direction.STILL

}

fun toggleState(gameState: GameState) {
    logger.severe { "Turn $turnCounter: Before switching from state $state to $gameState. " }
    if (gameState == state) {
        return
    }

    logger.severe { "Turn $turnCounter: Switching from state $state to $gameState. " }
    state = gameState
    //TODO state logic
}

fun detectBorder(loc: Location): TileLocation {
    var border = false


    for (neighbor in loc) {
        val nSite = gameMap!!.getSite(neighbor)
        if (nSite.owner != myId) {
            border = true
            if (nSite.strength == 0) {

                val isWarzone = Direction.CARDINALS
                        .map { gameMap!!.getSite(neighbor, it).owner }
                        .any { it != myId && it != 0 }

                if (isWarzone)
                    return TileLocation.WARZONE
            }
        }
    }
    if (border) return TileLocation.BORDER
    return TileLocation.INNER
}

fun getBestLocation(loc: Location): Location? {

    var locMax: Location? = null
    var heurMax = 0f


    var counter = 0
    graph.iteratorAt(loc).forEach { counter++}
    Logging.logger.severe { "Iterator foreach ends at $counter "}

    for (candidate in graph.iteratorAt(loc)) {
        Logging.logger.severe { "Trying $candidate for $loc." }
        val site = gameMap!!.getSite(candidate)
        if (site.owner != myId) {

            val candidateHeur = expansionHeuristic(loc)
            Logging.logger.severe { "Checking $candidate for origin $loc, max: $heurMax at $locMax" }
            if (heurMax < candidateHeur) {
                /* New max! */
                heurMax = candidateHeur
                locMax = candidate


            }
        }

    }

    return locMax


}

fun nearestEnemyDirNaive(loc: Location): Direction {
    val gameMap = gameMap!!

    val maxDistance = Math.min(gameMap.width, gameMap.height) / NEAREST_DIR_SEARCH_FACTOR
    for (i in 1..maxDistance) {

        /* Cardinals */
        logger.fine { "Displacing $loc by $i." }
        for (dir in Direction.CARDINALS) {
            val owner = gameMap.getSite(loc, dir, i).owner
            if (owner != 0 && owner != myId)
                return dir
        }


        /* Diagonals */
        logger.fine { "Diagonals $loc by $i." }
        if (i % 2 == 0) {
            for (dir in 1..NEAREST_SEARCH_LEVEL) {
                val locDiagonal = when (dir) {

                    1 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.EAST, i)
                    2 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.EAST, i)
                    3 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.WEST, i)
                    4 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.WEST, i)
                    5 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.EAST, i / 2)
                    6 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.WEST, i / 2)
                    7 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.EAST, i), Direction.NORTH, i / 2)
                    8 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.EAST, i), Direction.SOUTH, i / 2)
                    9 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.EAST, i / 2)
                    10 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.WEST, i / 2)
                    11 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.WEST, i), Direction.NORTH, i / 2)
                    12 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.WEST, i), Direction.SOUTH, i / 2)

                    else -> throw IllegalArgumentException("Weird diagonal naive search thing happened.")
                }
                val owner = gameMap.getSite(locDiagonal).owner
                if (owner != myId && owner != 0) return when (dir) {
                    1, 5, 6 -> Direction.NORTH
                    2, 7, 8 -> Direction.EAST
                    3, 9, 10 -> Direction.SOUTH
                    4, 11, 12 -> Direction.WEST
                //TODO make diagonals smarter
                    else -> throw IllegalArgumentException("What the hell happened here?")

                }

            }
        }
    }

    logger.severe { "Neutral dir naive search returned STILL for [${loc.x},${loc.y}]" }
    return Direction.STILL
}

fun nearestNeutralDirNaive(loc: Location): Direction {
    val gameMap = gameMap!!

    val maxDistance = Math.min(gameMap.width, gameMap.height) / NEAREST_DIR_SEARCH_FACTOR
    for (i in 1..maxDistance) {

        /* Cardinals */
        logger.fine { "Displacing $loc by $i." }
        Direction.CARDINALS
                .filter { gameMap.getSite(loc, it, i).owner != myId }
                .forEach { return it }


        /* Diagonals */
        logger.fine { "Diagonals $loc by $i." }
        if (i % 2 == 0) {
            for (dir in 1..NEAREST_SEARCH_LEVEL) {
                val locDiagonal = when (dir) {

                    1 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.EAST, i)
                    2 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.EAST, i)
                    3 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.WEST, i)
                    4 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.WEST, i)
                    5 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.EAST, i / 2)
                    6 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.WEST, i / 2)
                    7 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.EAST, i), Direction.NORTH, i / 2)
                    8 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.EAST, i), Direction.SOUTH, i / 2)
                    9 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.EAST, i / 2)
                    10 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.WEST, i / 2)
                    11 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.WEST, i), Direction.NORTH, i / 2)
                    12 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.WEST, i), Direction.SOUTH, i / 2)

                    else -> throw IllegalArgumentException("Weird diagonal naive search thing happened.")
                }

                if (gameMap.getSite(locDiagonal).owner != myId) return when (dir) {
                    1, 5, 6 -> Direction.NORTH
                    2, 7, 8 -> Direction.EAST
                    3, 9, 10 -> Direction.SOUTH
                    4, 11, 12 -> Direction.WEST
                //TODO make diagonals smarter
                    else -> throw IllegalArgumentException("What the hell happened here?")

                }

            }
        }
    }

    logger.severe { "Neutral dir naive search returned STILL for [${loc.x},${loc.y}]" }
    return Direction.STILL
}

fun heuristic(loc: Location): Float {
    val gameMap = gameMap!!
    val site = gameMap.getSite(loc)

    logger.fine("Owner of tile: " + site.owner)

    /* If neutral, return simple heuristic */
    if (site.owner == 0 && site.strength > 0) {


        val heuristic = site.production.toFloat() / site.strength.toFloat()
        return heuristic
    }

    /* If enemy, go by overkill damage */
    val totalDamage = Direction.CARDINALS
            .map { gameMap.getSite(loc, it) }
            .filter { it.owner != 0 && it.owner != myId }
            .sumBy(Site::strength)

    logger.fine(String.format(
            "Player tile: [%d, %d] prod: %d str: %d. damage: %d", loc.x,
            loc.y, site.production, site.strength, totalDamage))

    return totalDamage.toFloat()

}

fun expansionHeuristic(loc: Location): Float {
    val heur = processedMap!![loc.x][loc.y][0]

    logger.finer { "ExpHeur returning $heur for $loc " }
    return heur
}


/* Logging */
fun initLog(): Logger {
    Logging.init(LOGFILE_PREFIX, BOT_NAME, LOGFILE_SUFFIX, LOGGING_LEVEL)
    turnTimes = mutableListOf <Int>()
    return Logging.logger
}

private fun logTurn(startTime: Long) {
    val turnTimes = turnTimes!!
    turnTimes.add((System.currentTimeMillis() - startTime).toInt())
    logger.severe { "---------------------------------------TURN $turnCounter. Final state: $state" }
    /* Every 50 turns, display detailed output */
    if (turnCounter % 50 === 0) {
        val logMsg = StringBuilder()
        logMsg.append("Turn times so far: ")

        for (i in 0..turnCounter - 1) {
            logMsg.append(String.format("%dms, ", turnTimes[i]))
        }
        logger.info(logMsg.toString())
    }

}


/* OBSOLETE */
/*
fun nearestNeutralDir(loc: Location): Direction {
    val gameMap = gameMap!!
    var dir = Direction.STILL


    for (candidate in graph.iteratorBFS(loc)) {
        if (gameMap.getSite(candidate).owner != myId) {
            logger.severe { "Found a NearestNeutral! from: [${loc.x},${loc.y}] to [${candidate.x},${candidate.y}]." }
            return loc.directionTo(graph.path(candidate, loc).vertexList[1])
        }
    }


    if (dir == Direction.STILL)
        logger.warning(String.format(
                "Turn %d: [%d,%d] nearestNeutralDir returned STILL! ",
                turnCounter, loc.x, loc.y))

    return dir
}

fun nearestEnemyDir(loc: Location): Direction {
    val gameMap = gameMap!!
    var dir = Direction.STILL


    for (candidate in graph.iteratorBFS(loc)) {
        if (gameMap.getSite(loc).owner != myId && gameMap.getSite(loc).owner != 0)
            return loc.directionTo(graph.path(candidate, loc).vertexList[1])
    }


    if (dir == Direction.STILL)
        logger.warning(String.format(
                "Turn %d: [%d,%d] nearestEnemyDir returned STILL! ",
                turnCounter, loc.x, loc.y))

    return dir
}
*/

/* Should only be used for initial analysis */
fun qualityOfTile(site: Site): Float {
    val heur = site.production.toFloat() * PROD_IMPORTANCE_FACTOR / site.strength.toFloat() * (1 - PROD_IMPORTANCE_FACTOR)

    return heur
}

fun initMap() {
    val gameMap = gameMap!!

    processedMap = Array(gameMap.width) { Array(gameMap.height) { FloatArray(PROCESSING_PROPERTIES) } }
    qualityMap = Array(gameMap.width) { Array(gameMap.height) { FloatArray(PROCESSING_PROPERTIES) } }

    val processedMap = processedMap!!
    val qualityMap = qualityMap!!

    for (y in 0 until gameMap.height) {
        for (x in 0 until gameMap.width) {


            /* Process one tile */
            logger.finer { "Map dimensions [${gameMap.width}, ${gameMap.height}] Currently processingX [$x,$y]" }
            val site = gameMap.getSite(x, y)

            qualityMap[x][y][0] = qualityOfTile(site)
            qualityMap[x][y][1] = if (site.owner != 0 && site.owner != myId) site.strength.toFloat() else 0f

        }
    }
    /* Build map of blurred sites */
    for (y in 0..gameMap.height - 1) {
        for (x in 0..gameMap.width - 1) {

            var sumQ = 0f
            var sumA = 0f

            for (y1 in (BLUR_DIMENSION - 1) / 2 * -1..BLUR_DIMENSION - 1) {
                for (x1 in (BLUR_DIMENSION - 1) / 2 * -1..BLUR_DIMENSION - 1) {

                    // Translate to real location
                    val x2 = if (x1 + x < 0)
                        gameMap.width + x1 - 1
                    else if (x + x1 >= gameMap.width)
                        x + x1 - gameMap.width
                    else
                        x + x1

                    val y2 = if (y1 + y < 0)
                        gameMap.height + y1 - 1
                    else if (y + y1 >= gameMap.height)
                        y + y1 - gameMap.height
                    else
                        y + y1

                    val weight: Float = if (x1 == 0 && y1 == 0)
                        1f
                    else
                        1f / (1 + Math.abs(x1) + Math
                                .abs(y1)).toFloat()


                    logger.finer { "Parent: [$x,$y]. Neighbor, currently accessing: [$x2,$y2]" }
                    sumQ += qualityMap[x2][y2][0] * weight
                    sumA += qualityMap[x2][y2][1] * weight

                    logger.finer(String.format("Blur: [%d,%d] considering [%d,%d] with %f.3 weight and %f.5 sumQ",
                            x, y, x2, y2, weight, sumQ))

                }
            }


            processedMap[x][y][0] = sumQ + qualityMap[x][y][0]
            processedMap[x][y][1] = sumA + qualityMap[x][y][0]
        }
    }

    /* Use those maps to find sectors of interest */
    regionMap = Array(SECTORS_BY_SIDE) { Array(SECTORS_BY_SIDE) { FloatArray(PROCESSING_PROPERTIES) } }

    val regionMap = regionMap!!

    val sectorEdgeX = gameMap.width / SECTORS_BY_SIDE
    val sectorEdgeY = gameMap.height / SECTORS_BY_SIDE

    /* 0 x | 1 y | 2 quality | 3 x [army] | 4 y [army] | 5 army */
    val currentMostSector = FloatArray(6)

    for (i in 0..SECTORS_BY_SIDE - 1) {
        for (j in 0..SECTORS_BY_SIDE - 1) {

            var sumQ = 0f
            var sumA = 0f
            for (y in 0..sectorEdgeY - 1) {
                for (x in 0..sectorEdgeX - 1) {

                    sumQ += processedMap[i * sectorEdgeX + x][j * sectorEdgeY + y][0]
                    sumA += processedMap[i * sectorEdgeX + x][j * sectorEdgeY + y][1]


                }
            }

            val factor: Float = sectorEdgeX.toFloat() * sectorEdgeY.toFloat()

            regionMap[i][j][0] = sumQ / factor
            regionMap[i][j][1] = sumA / factor

            logger.info("Region completed. processedMap:\n"
                    + processedMap[i] + processedMap[i][j] + " "
                    + processedMap[i][j][0] + " " + processedMap[5][5][0])
            if (sumQ > currentMostSector[2]) {

                logger.info("Current most sector [2] is " + currentMostSector[2])
                currentMostSector[0] = i.toFloat()
                currentMostSector[1] = j.toFloat()
                currentMostSector[2] = sumQ

            }

            if (sumA > currentMostSector[5]) {
                currentMostSector[3] = i.toFloat()
                currentMostSector[4] = j.toFloat()
                currentMostSector[5] = sumA

            }

        }
    }

    logger.severe(String
            .format("Analysis complete. Most interesting regions:\n\t[%.0f,%.0f] Quality: %f\n\t[%f.0,%.0f] Army: %f",
                    currentMostSector[0], currentMostSector[1],
                    currentMostSector[2], currentMostSector[3],
                    currentMostSector[4], currentMostSector[5]))
}
