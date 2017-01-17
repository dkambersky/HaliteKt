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

}
