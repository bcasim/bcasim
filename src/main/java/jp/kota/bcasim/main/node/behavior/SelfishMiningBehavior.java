package jp.kota.bcasim.main.node.behavior;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.node.Node;

/** Extracted Attacker4 state machine. A factory must create a fresh instance per node. */
public final class SelfishMiningBehavior implements NodeBehavior {
    private Block targetBlock;
    private boolean stateZero;
    public void initialize(Node node) {
        targetBlock = node.getBlockchain().getLatestBlock();
        if (node.canMine()) node.StartMining(node.generateNewBlock());
    }
    public void receiveBlock(Node node, ReceiveBlock event) {
        Block block = event.getBlock();
        int difference = node.getDifferenceLen(targetBlock);
        node.PropagateBlock(block);
        node.addNewBlock(block);
        if (!node.canMine()) return;
        if (stateZero) {
            stateZero = false;
            targetBlock = node.getBlockchain().getLatestBlock();
            node.StartMining(node.generateNewBlock());
        }
        if (difference == 0) {
            node.PublishBlock();
            targetBlock = node.getBlockchain().getLatestBlock();
            node.StartMining(node.generateNewBlock());
        } else if (difference == 1) {
            node.PublishBlock();
            stateZero = true;
        } else if (difference == 2) {
            node.PublishBlock();
            targetBlock = node.getBlockchain().getLatestBlock();
            node.StartMining(node.generateNewBlock());
        }
    }
    public void foundBlock(Node node, FoundBlock event) {
        Block block = event.getBlock();
        int difference = node.getDifferenceLen(targetBlock);
        node.addUnpublishedBlocks(block);
        if (stateZero) {
            stateZero = false;
            node.PublishBlock();
            targetBlock = node.getBlockchain().getLatestBlock();
        }
        if (difference == 0 && node.getPublicBranch(targetBlock) == 2) {
            node.PublishBlock();
            targetBlock = node.getBlockchain().getLatestBlock();
        }
        if (node.canMine()) node.StartMining(node.generateNewBlock(block));
    }
}
