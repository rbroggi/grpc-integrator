package aggregator;

import model.Deal;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by rodrigobroggi on 05/05/18.
 */
public class Aggregator {

    private static final Logger logger = Logger.getLogger(Aggregator.class.getName());
    private Node root;

    @FunctionalInterface
    public interface IndexWiseOperation {
        public  List<Double> apply(List<Double> first, List<Double> second, BiFunction<Double,Double,Double> op);
    }

    private IndexWiseOperation indexWiseSafeOperation = (f, s, o) -> {
        return IntStream.range(0, f.size()).mapToObj( idx -> o.apply(f.get(idx), s.get(idx))).collect(Collectors.toList());
    };

    private IndexWiseOperation indexWiseOperation = (f, s, o) -> {
        if (f == null && s == null) {
            return null;
        } else if (f == null) {
            f = Stream.generate(() -> 0.0).limit(s.size()).collect(Collectors.toList());
        } else if (s == null) {
            s = Stream.generate(() -> 0.0).limit(f.size()).collect(Collectors.toList());
        }
        return indexWiseSafeOperation.apply(f,s,o);
    };


    private static class Node {
        private String id;
        private Map<String, Node> childs;
        private Node parent;
        private List<Double> exposures;

        public Node(String id) {
            this.id = id;
            childs = new TreeMap<>();
        }

        public Node(String id, Node parent) {
            this.id = id;
            this.parent = parent;
            childs = new TreeMap<>();
        }

        public String getId() {
            return id;
        }

        public Node getParent() {
            return parent;
        }

        public Map<String, Node> getChilds() {
            return childs;
        }

        public void setChilds(Map<String, Node> childs) {
            this.childs = childs;
        }

        public List<Double> getExposures() {
            return exposures;
        }

        public void setExposures(List<Double> exposures) {
            this.exposures = exposures;
        }

        public void addChild(Node child) {
            childs.putIfAbsent(child.getId(), child);
        }

        @Override
        public String toString() {
            return id + "|" + exposures;

        }
    }

    public Aggregator() {
        logger.info("Aggregator starting!");
        root = new Node("root");

    }

    synchronized public void aggregate(Deal deal) {
        Node ctp = root.getChilds().getOrDefault(deal.getCtp(), new Node(deal.getCtp(), root));
        root.addChild(ctp);
        Node contract = ctp.getChilds().getOrDefault(deal.getContract(), new Node(deal.getContract(), ctp));
        ctp.addChild(contract);
        Node existing_deal = contract.getChilds().getOrDefault(deal.getId(), new Node(deal.getId(), contract));
        contract.addChild(existing_deal);

        List<Double> deltaPrices = indexWiseOperation.apply(deal.getPricesList(), existing_deal.getExposures(), (a, b) -> a - b );
        existing_deal.setExposures(deal.getPricesList());
        contract.setExposures(indexWiseOperation.apply(contract.getExposures(), deltaPrices, (a, b) -> a + b));
        ctp.setExposures(indexWiseOperation.apply(ctp.getExposures(), deltaPrices, (a,b) -> a + b));
        root.setExposures(indexWiseOperation.apply(root.getExposures(), deltaPrices, (a,b) -> a + b));
        logTree();
    }

    private void logTree() {
        visitNode( root, node -> logger.log(Level.FINEST, () -> node.toString()));
    }

    private void visitNode( Node node, Consumer<Node> consumer) {
        consumer.accept(node);
        for (Node child : node.getChilds().values()) {
            visitNode(child, consumer);
        }
    }

}
