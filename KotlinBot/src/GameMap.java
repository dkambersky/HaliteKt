import java.util.Iterator;
import java.util.NoSuchElementException;

public class GameMap implements Iterable<Location>{

    public final Site[][] contents;
    public final Location[][] locations;
    public final int width, height;
    public static GameMap map;

    public GameMap(int width, int height, int[][] productions) {

        map = this;
        this.width = width;
        this.height = height;
        this.contents = new Site[width][height];
        this.locations = new Location[width][height];

        for (int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                final Site site = new Site(productions[x][y]);
                contents[x][y] = site;
                locations[x][y] = new Location(x, y);
            }
        }
    }

    public boolean inBounds(Location loc) {
        return loc.x < width && loc.x >= 0 && loc.y < height && loc.y >= 0;
    }

    public double getDistance(Location loc1, Location loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);

        if(dx > width / 2.0) dx = width - dx;
        if(dy > height / 2.0) dy = height - dy;

        return dx + dy;
    }

    public double getAngle(Location loc1, Location loc2) {
        int dx = loc1.x - loc2.x;

        // Flip order because 0,0 is top left
        // and want atan2 to look as it would on the unit circle
        int dy = loc2.y - loc1.y;

        if(dx > width - dx) dx -= width;
        if(-dx > width + dx) dx += width;

        if(dy > height - dy) dy -= height;
        if(-dy > height + dy) dy += height;

        return Math.atan2(dy, dx);
    }

    public Location getLocation(Location location, Direction direction) {
        switch (direction) {
            case STILL:
                return location;
            case NORTH:
                return locations[location.getX()][(location.getY() == 0 ? height : location.getY()) -1];
            case EAST:
                return locations[location.getX() == width - 1 ? 0 : location.getX() + 1][location.getY()];
            case SOUTH:
                return locations[location.getX()][location.getY() == height - 1 ? 0 : location.getY() + 1];
            case WEST:
                return locations[(location.getX() == 0 ? width : location.getX()) - 1][location.getY()];
            default:
                throw new IllegalArgumentException(String.format("Unknown direction %s encountered", direction));
        }
    }

    public Location getLocation(Location location, Direction direction, int displacement)  {
        if(displacement>24){
        Logging.logger.severe( String.format("Displacing %s to %s by %d", location,direction,displacement));}


//        int x, y;
//        switch (direction) {
//            case STILL:
//                return location;
//            case NORTH:
//                x = location.getX(); y = (location.getY()-displacement <= 0 ? height - (displacement-location.getY()) : location.getY()) - displacement;
//                break;
//            case EAST:
//                x = (location.getX()+displacement >=  width ?  (displacement-(width-location.getX())) : location.getX()) + displacement; y = location.getY(); break;
//            case SOUTH:
//                x = location.getX();  y = (location.getY()+displacement >=  height ?  (displacement-(height-location.getY())) : location.getY()) + displacement; break;
//            case WEST:
//                x = (location.getX()-displacement <= 0 ? width - (displacement-location.getX()) : location.getX() - displacement); y = location.getY(); break;
//
//            default:
//                throw new IllegalArgumentException(String.format("Unknown direction %s encountered", direction));
//        }
//        if(displacement>24){
//            Logging.logger.severe(String.format("Displacement found [%d, %d] Map dimensions %d * %d",x,y,width,height));
//        }
//        return locations[x][y];


        switch (direction) {
            case STILL:
                return location;
            case NORTH:
                return locations[location.getX()][location.getY()-displacement < 0 ? height - (displacement-location.getY()) : location.getY() - displacement];
            case EAST:
                return locations[location.getX()+displacement >=  width ?  displacement-(width-location.getX()) : location.getX() + displacement][location.getY()];
            case SOUTH:
                return locations[location.getX()][location.getY()+displacement >=  height ?  displacement-(height-location.getY()) : location.getY() + displacement];
            case WEST:
                return locations[location.getX()-displacement < 0 ? width - (displacement-location.getX()) : location.getX() - displacement][location.getX()];

            default:
                throw new IllegalArgumentException(String.format("Unknown direction %s encountered", direction));
        }
    }

    public Site getSite(Location loc, Direction dir) {
        Location target = getLocation(loc, dir);
        return contents[target.x][target.y];
    }

    public Site getSite(Location loc, Direction dir, int displacement){
        Location target = getLocation(loc,dir,displacement);
        return contents[target.x][target.y];
    }

    public Site getSite(Location loc) {
        return contents[loc.x][loc.y];
    }

    public Site getSite(int x, int y){
        return contents[x][y];
    }

    public Location getLocation(int x, int y) {
        return locations[x][y];
    }

    void reset() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final Site site = contents[x][y];
                site.owner = 0;
                site.strength = 0;
            }
        }
    }


    /* Iterator */
    public Iterator<Location> iterator(){
        return new GameMapIterator(this);
    }

    public static class GameMapIterator implements Iterator<Location> {

        private GameMap gameMap;
        private int i = -1;

        GameMapIterator(GameMap gameMap) {
            this.gameMap = gameMap;
        }

        @Override
        public boolean hasNext() {
            int y = (i + 1) / gameMap.width;
            return y < gameMap.height;
        }

        @Override
        public Location next() {

            i++;

            int y = i / gameMap.width;
            int x = i % gameMap.width;

            if (y >= gameMap.height) throw new NoSuchElementException();


            return new Location(x, y);
        }
    }
}

