/**
 * Created by David on 14/01/2017.
 */
fun main(args: Array<String>) {

    val iPackage = Networking.getInit()
    val myId = iPackage.myID
    val gameMap = iPackage.map

    Networking.sendInit("DavKotlinBot")

    while (true) {
        val moves = mutableListOf<Move>()

        Networking.updateFrame(gameMap)

        for (y in 0..gameMap.height) {
            for (x in 0..gameMap.width) {
                val location = gameMap.getLocation(x, y)
                val site = location.site

                if (site.owner == myId)
                    moves.add(Move(location, Direction.randomDirection()))
            }
        }
    }
}