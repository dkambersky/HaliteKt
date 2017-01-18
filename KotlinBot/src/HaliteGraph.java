import org.jgraph.graph.DefaultEdge;
import org.jgrapht.GraphPath;
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
            Logging.logger.finer(String.format("Adding [%d,%d] to graph", loc.x, loc.y));
            graphSimple.addVertex(loc);
        }

        for (Location loc : map) {
            for (Location neighbor : loc) {
                Logging.logger.finer(String.format("Adding edge [%d,%d] - [%d,%d] to graph", loc.x, loc.y, neighbor.x, neighbor.y));
                graphSimple.addEdge(loc, neighbor);

            }
        }

        /* Weighed graph */

    }

    public HaliteGraph() {

    }

    public List path(Location from, Location to) {

        return BidirectionalDijkstraShortestPath.findPathBetween
                (graphSimple, from, to);
    }

    public GraphPath pathVertex(Location from, Location to){
        return new BidirectionalDijkstraShortestPath<>(graphSimple, from, to)
                .getPath();
    }

    public GraphPath pathWeighed(Location from, Location to){
        return new BidirectionalDijkstraShortestPath<>(graph,from,to).getPath();
    }

}
