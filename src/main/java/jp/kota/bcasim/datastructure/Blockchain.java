package jp.kota.bcasim.datastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.Node;

/** Chain-local parent/child links and receipt times; payload propagation state is shared per run. */
public final class Blockchain {
    private final Simulation simulation;
    private final String chainID;
    private final Node node;
    private final Block genesisBlock;
    private Block topBlock;
    private final Map<String, Block> blocks = new LinkedHashMap<>();
    private final BlockCache blockCache = new BlockCache();

    public Blockchain(Simulation simulation, String chainID, Node node) {
        this.simulation = simulation; this.chainID = chainID; this.node = node;
        genesisBlock = Genesis.getGenesis(simulation); topBlock = genesisBlock;
        blocks.put(genesisBlock.getHash(), genesisBlock);
    }
    public void addBlock(Block incoming) {
        if (incoming.getMiner().getSimulation() != simulation) throw new IllegalArgumentException("Block belongs to another simulation");
        if (blocks.containsKey(incoming.getHash())) return;
        Block parent = blocks.get(incoming.getPreviousHash());
        if (parent == null) throw new IllegalArgumentException("Missing parent block: " + incoming.getPreviousHash());
        if (parent.getHeight() + 1 != incoming.getHeight()) throw new IllegalArgumentException("Invalid block height");
        Block block = Block.cloneBlock(incoming);
        block.setReceiveBlockTime(simulation.getScheduler().getSimulationTime());
        block.setPreviousBlock(parent);
        parent.addNextBlock(block);
        blocks.put(block.getHash(), block);
        blockCache.addBlock(block);
        // Aggregate chain uses null as its per-run recipient marker, matching the old implementation.
        boolean firstReceipt = block.getTransmittedNodes().isEmpty();
        block.addTransmittedNodes(node);
        if (firstReceipt) simulation.getWriter().recordBlock(block);
        topBlock = simulation.getForkChoice().select(chainID, topBlock, block);
    }
    public Block getLatestBlock() { return topBlock; }
    public Block getLatestBlock1() { return topBlock; }
    public Block serchBlockHash(String hash) { return blocks.get(hash); }
    public int getHeight() { return topBlock.getHeight(); }
    public String getChainID() { return chainID; }
    public Block getGenesis() { return genesisBlock; }
    public Block serchCacheHash(String hash) { return blockCache.serchHash(hash); }
    public Block serchCacheHeight(int height) { return blockCache.serchHeight(height); }
}
