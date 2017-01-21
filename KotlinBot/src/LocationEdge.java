import org.jgrapht.graph.DefaultEdge;

/**
 * shutup
 * Created by David on 20/01/2017.
 */
public class LocationEdge extends DefaultEdge {

    public Location from, to;
    private Site siteFrom, siteTo;

    public void setLoc(Location from, Location to) {
        this.from = from;
        this.to = to;
        siteFrom = GameMap.map.contents[from.x][from.y];
        siteTo = GameMap.map.contents[to.x][to.y];

    }


    public double getWeight() {
        if(to != null)
        return to.getWeight();


        Logging.logger.severe(String.format("getWeight didn't work. %s is probably null?", to));
        return HaliteMapGraph.DEFAULT_EDGE_WEIGHT;
    }

}
