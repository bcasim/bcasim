package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.HashGenerator;

public class PoW extends Consensus {
    private double hashrate;
    public PoW(Node node, double hashrate) { super(node); setHashrate(hashrate); }
    public Block generateBlock(Block previousBlock, double startTime) {
        String hash = HashGenerator.generateHash(String.valueOf(node.getSimulation().getIdentityRandom().nextDouble()) + previousBlock.getHash());
        Block block = new Block(hash, previousBlock, startTime + blocktime(), node, node.getTransactionPool().getTransactions(previousBlock));
        block.setPreviousBlock(previousBlock);
        return block;
    }
    public Block generateBlock(Block previousBlock) { return generateBlock(previousBlock, node.now()); }
    private double blocktime() {
        if (hashrate == 0) throw new IllegalStateException("A zero-weight node cannot mine");
        double mean = node.getSimulation().getConfig().getBlockInterval() / hashrate;
        return -mean * Math.log(1.0 - node.getSimulation().getMiningRandom().nextDouble());
    }
    public void setHashrate(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid mining weight");
        hashrate = value;
    }
    public double getHashrate() { return hashrate; }
    public double getWeight() { return hashrate; }
}
