package jp.kota.bcasim.main.node.behavior;

@FunctionalInterface
public interface NodeBehaviorFactory {
    NodeBehavior create(String strategy);
    static NodeBehaviorFactory builtIn() {
        return strategy -> {
            switch (strategy) {
                case "honest": return new HonestBehavior();
                case "selfish": return new SelfishMiningBehavior();
                case "double-spend": return new DoubleSpendingBehavior();
                default: throw new IllegalArgumentException("Unknown node strategy: " + strategy);
            }
        };
    }
}
