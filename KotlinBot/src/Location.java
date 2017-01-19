import java.util.Iterator;

public class Location implements Iterable<Location>  {

    // Public for backward compability
    public final int x, y;
    private final int no;


    Location(int x, int y) {
        this.x = x;
        this.y = y;
        no = x+(y*GameMap.map.width);



    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Iterator<Location> iterator(){
        return new LocNeighborIterator(this);
    }


    public Direction directionTo( Location nextTile) {




        // Same column
        if(x==nextTile.x){
            if (y == nextTile.y)
                return Direction.STILL;
            if(y == nextTile.y+1 )
                return Direction.NORTH;
            if(y == nextTile.y-1)
                return Direction.SOUTH;

            /* Edges */
            if(y==0 && nextTile.y==GameMap.map.height-1)
                return Direction.NORTH;

            if(nextTile.y==0 && y==GameMap.map.height-1)
                return Direction.SOUTH;


        }

        // Same row
        if(y == nextTile.y ){
            if (x == nextTile.x+1)
                return Direction.WEST;
            if(x==nextTile.x-1)
                return Direction.EAST;

            /* Edge (hehe) cases */
            if(x==0 && nextTile.x==GameMap.map.width-1)
                return Direction.WEST;

            if(nextTile.x==0 && x==GameMap.map.width-1)
                return Direction.EAST;

        }





        Logging.logger.severe("DirectionTo found a tile not directly adjacent. Bug?");
        return Direction.STILL;
}


    public static class LocNeighborIterator implements Iterator<Location>{
        private Location location;

        private int i;

        LocNeighborIterator(Location location) {
            this.location = location;
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i<4;

        }

        @Override
        public Location next() {

            i++;
            return GameMap.map.getLocation(location,Direction.values()[i]);

            }
    }

    @Override
    public int hashCode(){
        return no;
    }

    @Override
    public boolean equals(Object obj){
        return (obj instanceof Location && ((Location) obj).no == no);
    }

    public float getWeight(){
        Site site = GameMap.map.getSite(this);
        return (float)site.strength/(float)site.production;
    }

}
