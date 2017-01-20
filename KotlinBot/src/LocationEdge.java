import org.jgrapht.graph.DefaultEdge;

/**shutup
 * Created by David on 20/01/2017.
 */
public class LocationEdge extends DefaultEdge {

    private Location from, to;
    private Site siteFrom, siteTo;



    public LocationEdge(Location from, Location to) {
        this.from = from;
        this.to = to;
        siteFrom = GameMap.map.contents[from.x][from.y];
        siteTo = GameMap.map.contents[to.x][to.y];

    }


    public double getWeight(){
        return to.getWeight();
    }

}
