import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

/**
 * Created by David on 21/01/2017.
 */
public class HaliteMapGraph<V, E> extends DefaultDirectedWeightedGraph<V, E> {


    /**
     * Creates a new directed weighted graph.
     *
     * @param edgeClass class on which to base factory for edges
     */
    public HaliteMapGraph(Class<? extends E> edgeClass) {
        this(new ClassBasedEdgeFactory<>(edgeClass));
    }

    /**
     * Creates a new directed weighted graph with the specified edge factory.
     *
     * @param ef the edge factory of the new graph.
     */
    public HaliteMapGraph(EdgeFactory<V, E> ef) {
        super(ef);
    }

    @Override
    public double getEdgeWeight(Object obj) {

        if (obj instanceof LocationEdge) {
            LocationEdge edge = (LocationEdge) obj;
            double weight = edge.getWeight();
            Logging.logger.severe(String.format("From %s to %s weight %f", edge.from, edge.to, weight));
            return weight;
        }


        Logging.logger.severe("getEdgeWeight was supplied a non-LocationEdge object :(");
        return DEFAULT_EDGE_WEIGHT;

    }

}
