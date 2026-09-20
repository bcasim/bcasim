package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.HashGenerator;

/** The existing simplified deterministic stake-weighted interval model. */
public class PoS extends Consensus {
    private final double share;
    public PoS(Node node, double share) {
        super(node);
        if (!Double.isFinite(share) || share < 0) throw new IllegalArgumentException("Invalid stake weight");
        this.share = share;
    }
    public Block generateBlock(Block previousBlock, double startTime) {
        String hash = HashGenerator.generateHash(String.valueOf(node.getSimulation().getIdentityRandom().nextDouble()) + previousBlock.getHash());
        Block block = new Block(hash, previousBlock, startTime + blocktime(), node, node.getTransactionPool().getTransactions());
        block.setPreviousBlock(previousBlock);
        return block;
    }
    public Block generateBlock(Block previousBlock) { return generateBlock(previousBlock, node.now()); }
    public double getWeight() { return share; }
    public double blocktime() {
        if (share == 0) throw new IllegalStateException("A zero-weight node cannot mine");
        return node.getSimulation().getConfig().getBlockInterval() / share;
    }
}
