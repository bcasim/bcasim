package jp.kota.bcasim.datastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.transaction.LedgerState;

/** Chain-local parent/child links and receipt times; payload propagation state is shared per run. */
public final class Blockchain {
    private final Simulation simulation;
    private final String chainID;
    private final Node node;
    private final Block genesisBlock;
    private Block topBlock;
    private final Map<String, Block> blocks = new LinkedHashMap<>();
    private final BlockCache blockCache = new BlockCache();
    private long reorgCount;
    private int maxReorgDepth, forkPoints;

    public Blockchain(Simulation simulation, String chainID, Node node) {
        this.simulation = simulation; this.chainID = chainID; this.node = node;
        genesisBlock = Genesis.getGenesis(simulation); topBlock = genesisBlock;
        blocks.put(genesisBlock.getHash(), genesisBlock);
    }
    public void addBlock(Block incoming) {
        if (incoming == null || incoming.getMiner() == null || incoming.getMiner().getSimulation() != simulation)
            throw new IllegalArgumentException("Block belongs to another simulation");
        if (blocks.containsKey(incoming.getHash())) return;
        Block parent = blocks.get(incoming.getPreviousHash());
        LedgerState ledger = validate(incoming);
        incoming.setLedgerState(ledger);
        Block block = Block.cloneBlock(incoming);
        block.setLedgerState(ledger);
        block.setReceiveBlockTime(simulation.getScheduler().getSimulationTime());
        block.setPreviousBlock(parent);
        parent.addNextBlock(block);
        if (parent.getNextBlocks().size() == 2) forkPoints++;
        blocks.put(block.getHash(), block);
        blockCache.addBlock(block);
        // Aggregate chain uses null as its per-run recipient marker, matching the old implementation.
        boolean firstReceipt = block.getTransmittedNodes().isEmpty();
        block.addTransmittedNodes(node);
        if (firstReceipt) simulation.getWriter().recordBlock(block);
        Block oldTip = topBlock;
        topBlock = simulation.getForkChoice().select(chainID, topBlock, block);
        int detached = detachedDepth(oldTip, topBlock);
        if (detached > 0) { reorgCount++; maxReorgDepth = Math.max(maxReorgDepth, detached); }
    }
    /** Validate against this recipient's parent state; never trust an incoming balance snapshot. */
    private LedgerState validate(Block incoming) {
        if (incoming == null || incoming.getMiner() == null || incoming.getMiner().getSimulation() != simulation)
            throw new IllegalArgumentException("Block belongs to another simulation");
        Block parent = blocks.get(incoming.getPreviousHash());
        if (parent == null) throw new IllegalArgumentException("Missing parent block: " + incoming.getPreviousHash());
        if (incoming.getHash() == null || incoming.getHash().isEmpty() || parent.getHeight() + 1 != incoming.getHeight())
            throw new IllegalArgumentException("Invalid block identity or height");
        if (!Double.isFinite(incoming.getTimestamp()) || incoming.getTimestamp() < parent.getTimestamp())
            throw new IllegalArgumentException("Invalid block timestamp");
        if (incoming.getTransactionList().size() > Math.floor(simulation.getConfig().getBlockSize() / simulation.getConfig().getTransactionSize()))
            throw new IllegalArgumentException("Block exceeds transaction capacity");
        return parent.getLedgerState().applyBlock(incoming.getTransactionList(), incoming.getMiner().getName(), simulation.getConfig().getBlockReward());
    }
    public boolean canAccept(Block incoming) {
        try { validate(incoming); return true; }
        catch (IllegalArgumentException invalid) { return false; }
    }
    private static int detachedDepth(Block oldTip, Block newTip) {
        int depth = 0;
        while (newTip.getHeight() > oldTip.getHeight()) newTip = newTip.getPreviousBlock();
        while (oldTip.getHeight() > newTip.getHeight()) { oldTip = oldTip.getPreviousBlock(); depth++; }
        while (!oldTip.getHash().equals(newTip.getHash())) {
            oldTip = oldTip.getPreviousBlock(); newTip = newTip.getPreviousBlock(); depth++;
        }
        return depth;
    }
    public long getReorgCount() { return reorgCount; }
    public int getMaxReorgDepth() { return maxReorgDepth; }
    public int getForkPoints() { return forkPoints; }
    public java.util.List<Block> getBlocks() { return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(blocks.values())); }
    public Block getLatestBlock() { return topBlock; }
    public Block getLatestBlock1() { return topBlock; }
    public Block serchBlockHash(String hash) { return blocks.get(hash); }
    public int getHeight() { return topBlock.getHeight(); }
    public String getChainID() { return chainID; }
    public Block getGenesis() { return genesisBlock; }
    public Block serchCacheHash(String hash) { return blockCache.serchHash(hash); }
    public Block serchCacheHeight(int height) { return blockCache.serchHeight(height); }
}
