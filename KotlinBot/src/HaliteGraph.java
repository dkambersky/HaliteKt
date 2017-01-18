import org.jgrapht.GraphPath;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.alg.shortestpath.ListSingleSourcePathsImpl;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.ClosestFirstIterator;

import java.util.Iterator;

/**
 * shutup IDEA
 * Created by David on 17/01/2017.
 */
public class HaliteGraph {

    private WeightedGraph<Location,DefaultWeightedEdge> graph;



    private BidirectionalDijkstraShortestPath<Location,DefaultWeightedEdge> dijkstraWeighted;
    private BidirectionalDijkstraShortestPath<Location, DefaultWeightedEdge> dijkstraRadius;

    public HaliteGraph(GameMap map) {

        /* Base graph */
        graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class
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


    }

    public HaliteGraph() {

    }

    public GraphPath<Location, DefaultWeightedEdge> path(Location from, Location to) {
        return dijkstraWeighted().getPath(from,to);
    }

    public ListSingleSourcePathsImpl pathsToRadius(Location from){
        dijkstraRadius();
        Logging.logger.severe("DijkstraR: %s " + dijkstraRadius);

     return (ListSingleSourcePathsImpl) dijkstraRadius().
             getPaths(from);

    }

        private BidirectionalDijkstraShortestPath<Location, DefaultWeightedEdge> dijkstraWeighted() {
        if (dijkstraWeighted==null){
            dijkstraWeighted = new BidirectionalDijkstraShortestPath<>(graph);
        }
        return dijkstraWeighted;
    }

    private BidirectionalDijkstraShortestPath<Location, DefaultWeightedEdge> dijkstraRadius() {
        if (dijkstraRadius==null){
            dijkstraRadius = new BidirectionalDijkstraShortestPath<>(graph,MyBotKt.getSEARCH_RADIUS());
        }
        return dijkstraRadius;
    }

    public Iterator<Location> iteratorAt(Location loc){
        return new ClosestFirstIterator<>(graph, loc,MyBotKt.getSEARCH_RADIUS());

    }
}
