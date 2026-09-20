package jp.kota.bcasim.main.node;

import java.util.ArrayList;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import jp.kota.bcasim.datastructure.*;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.event.*;
import jp.kota.bcasim.main.node.behavior.NodeBehavior;
import jp.kota.bcasim.main.node.consensus.Consensus;

/** Node state and common transport/mining operations; decisions live in NodeBehavior. */
public class Node {
    private final Simulation simulation;
    private final String nodeID;
    private final Consensus consensus;
    private final NodeBehavior behavior;
    protected final Blockchain blockchain;
    protected final ArrayList<Block> unpublishedBlocks = new ArrayList<>();
    private final TransactionPool transactionPool;
    private final Map<String, ReceiveBlock> orphanBlocks = new LinkedHashMap<>();
    private long rejectedBlocks;

    public Node(Simulation simulation, String name, double weight, NodeBehavior behavior) {
        this.simulation = Objects.requireNonNull(simulation, "simulation");
        this.nodeID = Objects.requireNonNull(name, "name");
        if (name.isEmpty() || !Double.isFinite(weight) || weight < 0) throw new IllegalArgumentException("Invalid node configuration");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        blockchain = new Blockchain(simulation, name, this);
        transactionPool = new TransactionPool(simulation.getConfig());
        consensus = Objects.requireNonNull(simulation.getConsensusFactory().create(this, weight), "consensus");
    }
    /** Genesis is a per-run identity, without a recursively constructed ledger. */
    protected Node(Simulation simulation, String name) {
        this.simulation = simulation; nodeID = name;
        behavior = new NodeBehavior() {}; blockchain = null; transactionPool = null; consensus = null;
    }
    public void initNode(Event event) { behavior.initialize(this); }
    public void receiveBlock(Event event) {
        ArrayDeque<ReceiveBlock> ready = new ArrayDeque<>();
        ready.add((ReceiveBlock) event);
        while (!ready.isEmpty()) {
            ReceiveBlock received = ready.removeFirst();
            Block block = received.getBlock();
            if (block == null || block.getMiner() == null || block.getMiner().getSimulation() != simulation ||
                block.getHash() == null || block.getHash().isEmpty()) { rejectedBlocks++; continue; }
            if (blockchain.serchBlockHash(block.getPreviousHash()) == null) {
                orphanBlocks.putIfAbsent(block.getHash(), received);
                continue;
            }
            if (!blockchain.canAccept(block)) { rejectedBlocks++; continue; }
            behavior.receiveBlock(this, received);
            java.util.Iterator<ReceiveBlock> waiting = orphanBlocks.values().iterator();
            while (waiting.hasNext()) {
                ReceiveBlock orphan = waiting.next();
                if (blockchain.serchBlockHash(orphan.getBlock().getPreviousHash()) != null) {
                    ready.add(orphan); waiting.remove();
                }
            }
        }
    }
    public void foundBlock(Event event) { behavior.foundBlock(this, (FoundBlock) event); }
    public void receiveTransaction(Event event) {
        ReceiveTransaction received = (ReceiveTransaction) event;
        if (acceptTransaction(received.getTransaction())) {
            PropagateTransaction(received.getTransaction());
            behavior.receiveTransaction(this, received);
        }
    }
    public void sendTransaction(Event event) {
        SendTransaction sent = (SendTransaction) event;
        if (acceptTransaction(sent.getTransaction())) {
            PropagateTransaction(sent.getTransaction());
            behavior.sendTransaction(this, sent);
        }
    }
    private boolean acceptTransaction(Transaction transaction) {
        if (!transactionPool.accept(transaction, blockchain.getLatestBlock())) return false;
        transaction.addTransmittedNodes(this);
        return true;
    }
    public Simulation getSimulation() { return simulation; }
    public double now() { return simulation.getScheduler().getSimulationTime(); }
    public double getHashrate() { return consensus == null ? 0 : consensus.getWeight(); }
    public boolean canMine() { return getHashrate() > 0; }
    public Consensus getConsensus() { return consensus; }
    public NodeBehavior getBehavior() { return behavior; }
    public void PropagateBlock(Block block) {
        for (Node destination : simulation.getNetwork().getAdjacencyNode(nodeID)) {
            if (!block.verifyNode(destination)) simulation.getScheduler().addNewEvent(
                new ReceiveBlock(now() + simulation.getNetwork().getBlockDelay(), destination, block, this));
        }
    }
    public void PropagateTransaction(Transaction transaction) {
        for (Node destination : simulation.getNetwork().getAdjacencyNode(nodeID)) {
            if (!transaction.verifyNode(destination)) simulation.getScheduler().addNewEvent(
                new ReceiveTransaction(now() + simulation.getNetwork().getTransactionDelay(), destination, transaction, this));
        }
    }
    public void PublishBlock() {
        for (Block block : unpublishedBlocks) {
            addNewBlock(block);
            for (Node destination : simulation.getNetwork().getAdjacencyNode(nodeID)) simulation.getScheduler().addNewEvent(
                new ReceiveBlock(now() + simulation.getNetwork().getBlockDelay(), destination, block, this));
        }
        unpublishedBlocks.clear();
    }
    public void StartMining(Block block) {
        if (canMine()) simulation.getScheduler().addNewEvent(new FoundBlock(block.getTimestamp(), this, block));
    }
    public void StopMining() {
        FoundBlock event = simulation.getScheduler().getFoundEvent(this);
        if (event != null) simulation.getScheduler().removeEvent(event.getEventID());
    }
    private Block publicTip() {
        if (!unpublishedBlocks.isEmpty()) throw new IllegalStateException("Specify a private parent when mining unpublished blocks");
        return blockchain.getLatestBlock();
    }
    public Block generateNewBlock(double startTime) { return consensus.generateBlock(publicTip(), startTime); }
    public Block generateNewBlock() { return consensus.generateBlock(publicTip()); }
    public Block generateNewBlock(Block parent) { return consensus.generateBlock(parent); }
    public void addNewBlock(Block block) {
        Block oldTip = blockchain.getLatestBlock();
        simulation.getScheduler().addBlock(block);
        blockchain.addBlock(block);
        transactionPool.reconcile(oldTip, blockchain.getLatestBlock());
    }
    public void addTransaction(Transaction transaction) { acceptTransaction(transaction); }
    public Transaction geTransaction() { return transactionPool.popTransaction(); }
    public Blockchain getBlockchain() { return blockchain; }
    public String getName() { return nodeID; }
    public void addUnpublishedBlocks(Block block) { unpublishedBlocks.add(block); }
    public int getHeightunpublishedBlock() { return unpublishedBlocks.isEmpty() ? -1 : unpublishedBlocks.get(0).getHeight(); }
    public void clearUnpublishedBlocks() { unpublishedBlocks.clear(); }
    public int getDifferenceLen(Block vertex) {
        int local = vertex == null ? 0 : vertex.getHeight();
        if (!unpublishedBlocks.isEmpty()) local = unpublishedBlocks.get(unpublishedBlocks.size() - 1).getHeight();
        return local - blockchain.getLatestBlock().getHeight();
    }
    /** Historical method name: private branch length from the fork point. */
    public int getPublicBranch(Block vertex) {
        int local = unpublishedBlocks.isEmpty() ? vertex.getHeight() : unpublishedBlocks.get(unpublishedBlocks.size() - 1).getHeight();
        return local - vertex.getHeight();
    }
    /** Historical method name: public branch length from the fork point. */
    public int getPrivateBranch(Block vertex) { return blockchain.getLatestBlock().getHeight() - vertex.getHeight(); }
    public void outputLog(String message) { simulation.getWriter().recordAttack(message); }
    public TransactionPool getTransactionPool() { return transactionPool; }
    public long getRejectedBlockCount() { return rejectedBlocks; }
    public int getOrphanBlockCount() { return orphanBlocks.size(); }
    /** On reconnection request public history parent-first. Withheld private blocks remain private. */
    public void synchronizeWith(Node peer) {
        if (peer == null || peer.getSimulation() != simulation) throw new IllegalArgumentException("Peer belongs to another simulation");
        if (!simulation.getNetwork().isConnected(peer, this)) return;
        for (Block block : peer.getBlockchain().getBlocks()) {
            if (block.getHeight() > 0 && blockchain.serchBlockHash(block.getHash()) == null)
                simulation.getScheduler().addNewEvent(new ReceiveBlock(now() + simulation.getNetwork().getBlockDelay(), this, block, peer));
        }
    }
}
