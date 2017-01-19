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

when(state){

    /* Early game, expanding
     * TODO figure out diff between initial/expanding
     */
    GameState.INITIAL,
    GameState.EXPANDING -> {
        when(location){
            TileLocation.BORDER ->{
                /* Early game, exansion, on the border */

            }

            TileLocation.INNER -> {
                /* Early game, expansion, inner cell */

            }

            TileLocation.WARZONE -> {
                /* Encountered an enemy for the first time! */
                toggleState(GameState.COMBAT)


            }

        }



    }

    /* We're fighting players */
    GameState.COMBAT -> {
        when (location){
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

            }
        }
    }
}


    /* OLD StUFF*/

    /* No-brainers - wait for production, don't move 0 strength tiles */
    if(site.strength<=site.production*PROD_MULTIPLIER) return Direction.STILL

    /* Combat */
    var isOnBorder = false
    var isOnEnemyBorder = false



    /* Fight if required */
    if(isOnBorder){
    if(isOnEnemyBorder) {

        val bestTarget:Location? = loc.maxBy(::heuristic)
        toggleState(GameState.COMBAT)

        return loc.directionTo(bestTarget)
    }
        /* Capturing neutral territory*/
        if(state!=GameState.COMBAT) {
            val bestTarget: Location? = loc.maxBy(::heuristic)

            if(site.strength>gameMap.getSite(bestTarget).strength)
            return loc.directionTo(bestTarget)
        }

    }




    /* Debug */
    val dest = getBestLocation(loc) ?: Location(-1,-1)
    if(dest.x==-1) return Direction.NORTH

    logger.severe{ "$turnCounter | Tile: [${loc.x}, ${loc.y}]  Dest : [${dest.x},${dest.y}]" }

    val path = graph.path(loc, dest)
    val nextTile = path.vertexList[1]
    val nextSite = gameMap.getSite(nextTile)

        logger.fine { ("Turn $turnCounter | Loc: [${loc.x},${loc.y}] path.first: ${nextTile.x},${nextTile.y}") }

        if (site.strength < nextSite.strength && nextSite.owner == 0) return Direction.STILL
        return loc.directionTo(nextTile)







    /* End debug */





    /* Detect borders */
    var highestHeurDir = Direction.STILL
    val border = false

    for (dir in Direction.CARDINALS) {
        val neighLoc = gameMap.getLocation(loc, dir)
        val neighbor = gameMap.getSite(neighLoc)


        if (neighbor.owner != myId) {
            border = true

            /* Initialize highest prod neighbor */
            if (highestHeurDir == Direction.STILL)
                highestHeurDir = dir

            /* Note higher production neighbors */
            if (heuristic(neighLoc) > heuristic(gameMap.getLocation(loc,
                    highestHeurDir)))
                highestHeurDir = dir

        }
    }


    /* Attack weaker neighbours */
    if (border && gameMap.getSite(loc, highestHeurDir).strength < site.strength)
        return highestHeurDir


    /* If inside and not producing, move */
    if (!border)
        return nearestEnemyDir(loc)

    /* Fail-safe */
    logger.warning(String
            .format("Turn %d: [%d, %d] Couldn't come up with a move. Standing still.",
                    turnCounter, loc.x, loc.y))
    return Direction.STILL

}

fun detectBorder(loc: Location): TileLocation {
    var border = false
    for(neighbor in loc) {
        val nSite = gameMap!!.getSite(neighbor)
        if (nSite.owner != myId) {
            border = true
            if (nSite.strength == 0) {
                // TODO 0 str neutrals not warzones
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


    var maxDistance = Math.min(gameMap.width, gameMap.height) / NEAREST_DIR_SEARCH_FACTOR

    for (d in Direction.CARDINALS) {
        var distance = 0
        var current = loc
        var site = gameMap.getSite(loc)

        while (site.owner == myId && distance < maxDistance) {
            distance++
            current = gameMap.getLocation(current, d)
            site = gameMap.getSite(current)
        }

        if (distance < maxDistance) {
            dir = d
            maxDistance = distance
        }
    }

    if (dir == Direction.STILL)
        logger.warning(String.format(
                "Turn %d: [%d,%d] nearestEnemyDir returned STILL! ",
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

fun  toggleState(gameState: GameState) {
    if(gameState==state) return

    logger.severe {"Turn $turnCounter: Switching from state $state to $gameState. "}
    state = gameState
    //TODO state logic
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



