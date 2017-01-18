import org.jgraph.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.BidirectionalDijkstraShortestPath;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;

/**
 * shutup IDEA
 * Created by David on 17/01/2017.
 */
public class HaliteGraph {
    UndirectedGraph graphSimple;
    WeightedGraph graph;
    BidirectionalDijkstraShortestPath<UndirectedGraph, DefaultEdge> dijkstraSimple;

    public HaliteGraph(GameMap map) {

        /* Base graph */
        graphSimple = new SimpleGraph<Location, DefaultEdge>(DefaultEdge.class);

        for (Location loc : map) {
            Logging.logger.severe(String.format("Adding [%d,%d] to graph", loc.x, loc.y));
            graphSimple.addVertex(loc);
        }

        for (Location loc : map) {
            for (Location neighbor : loc) {
                Logging.logger.severe(String.format("Adding edge [%d,%d] - [%d,%d] to graph", loc.x, loc.y, neighbor.x, neighbor.y));
                graphSimple.addEdge(loc, neighbor);

            }
        }
    }

    public HaliteGraph() {

    }

    public List path(Location from, Location to) {
        long start = System.currentTimeMillis();
        return BidirectionalDijkstraShortestPath.findPathBetween
                (graphSimple, from, to);
    }

}
