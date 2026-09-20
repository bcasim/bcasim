package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.main.node.Node;

@FunctionalInterface
public interface ConsensusFactory {
    Consensus create(Node node, double weight);
    static ConsensusFactory configured() {
        return (node, weight) -> "PoS".equals(node.getSimulation().getConfig().getConsensus())
            ? new PoS(node, weight) : new PoW(node, weight);
    }
}
