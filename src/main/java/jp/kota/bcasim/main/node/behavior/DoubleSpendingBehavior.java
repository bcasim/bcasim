package jp.kota.bcasim.main.node.behavior;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.node.Node;

/** Extracted Attacker3 behavior. Confirmation and abandonment thresholds are injectable. */
public final class DoubleSpendingBehavior implements NodeBehavior {
    private final int confirmations, abandonmentDepth;
    private Block targetBlock;
    private int successes, failures;
    public DoubleSpendingBehavior() { this(6, 50); }
    public DoubleSpendingBehavior(int confirmations, int abandonmentDepth) {
        if (confirmations < 1 || abandonmentDepth < 1) throw new IllegalArgumentException("Thresholds must be positive");
        this.confirmations = confirmations; this.abandonmentDepth = abandonmentDepth;
    }
    public void receiveBlock(Node node, ReceiveBlock event) {
        node.addNewBlock(event.getBlock());
        if (!node.canMine()) return;
        if (targetBlock == null) {
            targetBlock = event.getBlock();
            if (node.canMine()) node.StartMining(node.generateNewBlock(targetBlock));
        }
        if (node.getDifferenceLen(targetBlock) < -abandonmentDepth) {
            failures++;
            node.outputLog("fail");
            node.clearUnpublishedBlocks();
            targetBlock = node.getBlockchain().getLatestBlock();
            if (node.canMine()) node.StartMining(node.generateNewBlock(targetBlock));
        }
    }
    public void foundBlock(Node node, FoundBlock event) {
        node.addUnpublishedBlocks(event.getBlock());
        if (node.getPublicBranch(targetBlock) >= confirmations && node.getDifferenceLen(targetBlock) >= 0) {
            successes++;
            node.outputLog("succ");
            node.PublishBlock();
            targetBlock = node.getBlockchain().getLatestBlock();
            if (node.canMine()) node.StartMining(node.generateNewBlock(targetBlock));
        } else if (node.canMine()) node.StartMining(node.generateNewBlock(event.getBlock()));
    }
    public int getSuccesses() { return successes; }
    public int getFailures() { return failures; }
}
