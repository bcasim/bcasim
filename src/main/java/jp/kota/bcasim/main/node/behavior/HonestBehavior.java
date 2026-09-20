package jp.kota.bcasim.main.node.behavior;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.node.Node;

/** Reference node behavior, retaining the existing mining restart rule. */
public final class HonestBehavior implements NodeBehavior {
    public void initialize(Node node) { if (node.canMine()) node.StartMining(node.generateNewBlock(node.now())); }
    public void receiveBlock(Node node, ReceiveBlock event) {
        Block block = event.getBlock();
        node.PropagateBlock(block);
        FoundBlock pending = node.getSimulation().getScheduler().getFoundEvent(node);
        int pendingHeight = pending == null ? 0 : pending.getBlock().getHeight();
        node.addNewBlock(block);
        if (node.canMine() && block.getHeight() + 1 >= pendingHeight) node.StartMining(node.generateNewBlock(node.now()));
    }
    public void foundBlock(Node node, FoundBlock event) {
        node.addUnpublishedBlocks(event.getBlock());
        node.PublishBlock();
        if (node.canMine()) node.StartMining(node.generateNewBlock(node.now()));
    }
}
