import com.sun.org.apache.xml.internal.security.Init
import java.util.logging.FileHandler
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
 *
 *  INTERMEDIATE
 *   - move strength where it's needed
 *   - attacking through combining cells
 *
 *  LARGE, FUNDAMENTAL
 *   - move towards high prod, low str stuff
 *   - map analysis
 *	  - A* / Dijtskra (BFS??)
 *
 */


/* Constants */
val PROD_MULTIPLIER = 5
val NEAREST_DIR_SEARCH_FACTOR = 1


/* Logging */
val LOGGING_LEVEL = Level.INFO
val BOT_NAME = "DavKotlinBot_v1"
val LOGFILE_PREFIX = "../logs/"
val LOGFILE_SUFFIX = ".txt"

/* Map Processing */

/*
 * should be a multiple of 5 because of conventional map dimensions
 */
val SECTORS_BY_SIDE = 10;

/*
 * size of the edge of the blur applied
 */
val  BLUR_DIMENSION = 3;

/*
 * 0 desirability | 1 enemy army strength | 2 friendly power arriving
 */
val PROCESSING_PROPERTIES = 3;


/* Globals */
var myId = 0
var gameMap = GameMap(1,1,null) // Dummy map so we don't have to deal with null safety
var turnCounter = 0

/* Algorithm stuff */
var processedMap = arrayOf(Float,Float,Float)
var qualityMap = arrayOf(Float,Float,Float)
var regionMap = arrayOf(Float,Float,Float)

/* Logging */
val logger = Logger.getLogger(BOT_NAME)
val handler:FileHandler? = null


fun main(args: Array<String>) {

    val startTime = System.currentTimeMillis()

    /* Initialization */
    initLog()

    val iPackage = Networking.getInit()
    myId = iPackage.myID
    gameMap = iPackage.map

    initMap()

    Networking.sendInit("DavKotlinBot")
    logger.info("Initialization took ${System.currentTimeMillis()-startTime/1000} seconds.")



    while (true) {
        val moves = mutableListOf<Move>()
        Networking.updateFrame(gameMap)

        for (y in 0..gameMap.height-1) {
            for (x in 0..gameMap.width-1) {
                val location = gameMap.getLocation(x, y)
                val site = location.site

                if (site.owner == myId) moves.add(Move(location, Direction.randomDirection()))
            }
        }
        Networking.sendFrame(moves)
}
}

fun initMap() {

}

fun initLog() {

}
