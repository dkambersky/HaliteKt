import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.alg.shortestpath.ListSingleSourcePathsImpl;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.ClosestFirstIterator;

import java.util.Iterator;

/**
 * shutup IDEA
 * Created by David on 17/01/2017.
 */
public class HaliteGraph {

    private static final float WEIGHT_PER_MOVE = 0.25f;
    private HaliteMapGraph<Location, LocationEdge> graph;
    private BidirectionalDijkstraShortestPath<Location, LocationEdge> dijkstraWeighted;
    private BidirectionalDijkstraShortestPath<Location, LocationEdge> dijkstraRadius;

    public HaliteGraph(GameMap map) {

        /* Base graph */
        graph = new HaliteMapGraph<>(LocationEdge.class);

        for (Location loc : map) {
            Logging.logger.finer(String.format("Adding [%d,%d] to graph", loc.x, loc.y));
            graph.addVertex(loc);
        }

        for (Location loc : map) {
            for (Location neighbor : loc) {
                Logging.logger.finer(String.format("Adding edge [%d,%d] - [%d,%d] to graph",
                        loc.x, loc.y, neighbor.x, neighbor.y));

                LocationEdge edge = graph.addEdge(loc, neighbor);

                if (edge !=null ) {edge.setLoc(loc,neighbor);}

                LocationEdge edge2 = graph.addEdge(neighbor, loc);
                if (edge != null ) {edge2.setLoc(neighbor,loc);}

            }
        }


    }

    public HaliteGraph() {

    }

    public GraphPath<Location, LocationEdge> path(Location from, Location to) {
        return dijkstraWeighted().getPath(from, to);
    }

    public ListSingleSourcePathsImpl pathsToRadius(Location from) {
        // TODO not sure if dijkstraRadius is initialized correctly

        return (ListSingleSourcePathsImpl) dijkstraRadius().
                getPaths(from);

    }

    private BidirectionalDijkstraShortestPath<Location, LocationEdge> dijkstraWeighted() {
        if (dijkstraWeighted == null) {
            dijkstraWeighted = new BidirectionalDijkstraShortestPath<>(graph);
        }
        return dijkstraWeighted;
    }

    private BidirectionalDijkstraShortestPath<Location, LocationEdge> dijkstraRadius() {
        if (dijkstraRadius == null) {
            dijkstraRadius = new BidirectionalDijkstraShortestPath<>(graph, MyBotKt.getSEARCH_RADIUS());
        }
        return dijkstraRadius;
    }

    public Iterator<Location> iteratorAt(Location loc) {
        return new ClosestFirstIterator<>(graph, loc, MyBotKt.getSEARCH_RADIUS());

    }

    public Iterator<Location> iteratorBFS(Location loc) {
        return new BreadthFirstIterator<>(graph, loc);
    }
}
