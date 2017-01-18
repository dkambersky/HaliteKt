import org.jgraph.graph.DefaultEdge;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.BidirectionalDijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

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
        graph = new SimpleWeightedGraph<Location, DefaultWeightedEdge>(DefaultWeightedEdge.class
        );

        for (Location loc : map) {
            Logging.logger.finer(String.format("Adding [%d,%d] to graph", loc.x, loc.y));
            graph.addVertex(loc);
        }

        for (Location loc : map) {
            for (Location neighbor : loc) {
                Logging.logger.finer(String.format("Adding edge [%d,%d] - [%d,%d] to graph",
                        loc.x, loc.y, neighbor.x, neighbor.y));

                DefaultWeightedEdge edge = new DefaultWeightedEdge();
                graph.addEdge(loc, neighbor);
                graph.setEdgeWeight(edge,loc.getWeight());


            }
        }

        /* Weighed graph */

    }

    public HaliteGraph() {

    }


    public GraphPath pathVertex(Location from, Location to) {
        return new BidirectionalDijkstraShortestPath<>(graphSimple, from, to)
                .getPath();
    }

    public GraphPath pathWeighted(Location from, Location to) {
        return new BidirectionalDijkstraShortestPath<>(graph, from, to).getPath();
    }


}
