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
val SEARCH_RADIUS = 5

/* Logging */
val LOGGING_LEVEL: Level = Level.WARNING
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

    Networking.sendInit("DavKotlinBot")
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

                if (site.owner == myId) moves.add(Move(location, nextMove(location)))
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
                TileLocation.BORDER -> {
                    /* Early game, expansion, on the border */

                    val dest = getBestLocation(loc) ?: Location(-1, -1)
                    if (dest.x == -1) return Direction.NORTH //TODO what happens when BestLocation finds nothing?

                    logger.severe { "$turnCounter | Tile: [${loc.x}, ${loc.y}]  Dest : [${dest.x},${dest.y}]" }

                    val path = graph.path(loc, dest)
                    val nextTile = path.vertexList[1]


                    logger.fine { ("Turn $turnCounter | Loc: [${loc.x},${loc.y}] path.first: ${nextTile.x},${nextTile.y}") }


                    if (site.strength >= gameMap.getSite(nextTile).strength)
                        return loc.directionTo(nextTile)

                }

                TileLocation.INNER -> {
                    /* Early game, expansion, inner cell */
                    return nearestNeutralDirNaive(loc)
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

                }

                TileLocation.BORDER -> {
                    /* Send reinforcements or expand
                     * TODO figure out when to prioritize what
                     */


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

    return graph.iteratorAt(loc).asSequence().filter { GameMap.map.getSite(it).owner != myId }.maxBy { GameMap.map.getSite(it).production }

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


fun nearestNeutralDirNaive(loc: Location): Direction {
    val gameMap = gameMap!!

    var maxDistance = Math.min(gameMap.width, gameMap.height) / NEAREST_DIR_SEARCH_FACTOR
    for (i in 1..maxDistance) {

        /* Cardinals */
        logger.severe { "Displacing $loc by $i." }
        Direction.CARDINALS
                .filter {  gameMap.getSite(loc, it, i).owner != myId }
                .forEach { return it }


        /* Diagonals */
        logger.severe { "Diagonals $loc by $i." }
        if(i % 2 ==0){
            for(dir in 1..12){
                val locDiagonal = when(dir) {
                    1 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.EAST, i / 2)
                    2 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.WEST, i / 2)
                    3 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.EAST, i), Direction.NORTH, i / 2)
                    4 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.EAST, i), Direction.SOUTH, i / 2)
                    5 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.EAST, i / 2)
                    6 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.WEST, i / 2)
                    7 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.WEST, i), Direction.NORTH, i / 2)
                    8 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.WEST, i), Direction.SOUTH, i / 2)
                    9 -> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.EAST, i )
                    10-> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.EAST, i )
                    11-> gameMap.getLocation(gameMap.getLocation(loc, Direction.SOUTH, i), Direction.WEST, i )
                    12-> gameMap.getLocation(gameMap.getLocation(loc, Direction.NORTH, i), Direction.WEST, i )
                    else -> throw IllegalArgumentException("Weird diagonal naive search thing happened.")
                }

                if(gameMap.getSite(locDiagonal).owner!=myId)  return when(dir){
                    1,2,9 -> Direction.NORTH
                    3,4,10 -> Direction.EAST
                    5,6,11 -> Direction.SOUTH
                    7,8,12 -> Direction.WEST
                    //TODO make diagonals smarter
                    else -> throw IllegalArgumentException("What the hell happened here?")

                }

            }
        }
    }

    logger.severe { "Neutral dir naive search returned STILL for [${loc.x},${loc.y}]" }
    return Direction.STILL
}

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


/* Should only be used for initial analysis */
fun qualityOfTile(site: Site): Float {
    return (if (site.strength == 0)
        site.production / site.strength
    else
        site.production).toFloat()
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

            // logger.info(String.format(
            // "processing! [%d, %d] Q: %f | A: %f \n\n", x, y, sumQ,
            // sumA));
            processedMap[x][y][0] = sumQ
            processedMap[x][y][1] = sumA
        }
    }

    /* Use those maps to find sectors of interest */
    regionMap = Array(SECTORS_BY_SIDE) { Array(SECTORS_BY_SIDE) { FloatArray(PROCESSING_PROPERTIES) } }

    val regionMap = regionMap!!

    val sectorEdgeX = gameMap.width / SECTORS_BY_SIDE
    val sectorEdgeY = gameMap.height / SECTORS_BY_SIDE
    val currentMostSector = FloatArray(6) // 0 x | 1 y | 2 quality | 3 x
    // [army] | 4 y [army] | 5
    // army

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



