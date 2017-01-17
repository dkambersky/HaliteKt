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
            graphSimple.addVertex(loc);
        }

        for (Location loc : map) {
            for (Location neighbor : loc) {
                graphSimple.addEdge(loc, neighbor);

            }
        }
    }

    public List path(Location from, Location to) {
        long start = System.currentTimeMillis();
        return BidirectionalDijkstraShortestPath.findPathBetween
                (graphSimple, from, to);
    }

}
