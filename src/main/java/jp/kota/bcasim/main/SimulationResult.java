package jp.kota.bcasim.main;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.network.Network;

/** Immutable summary for batch experiments. */
public final class SimulationResult {
    private final long seed, processedEvents;
    private final double finalTime;
    private final Map<String, String> chainTips;
    private final Map<String, Integer> chainHeights;
    SimulationResult(long seed, double time, long events, Network network) {
        this.seed = seed; finalTime = time; processedEvents = events;
        Map<String, String> tips = new LinkedHashMap<>();
        Map<String, Integer> heights = new LinkedHashMap<>();
        for (Node node : network.getNodeList()) {
            tips.put(node.getName(), node.getBlockchain().getLatestBlock().getHash());
            heights.put(node.getName(), node.getBlockchain().getHeight());
        }
        chainTips = Collections.unmodifiableMap(tips);
        chainHeights = Collections.unmodifiableMap(heights);
    }
    public long getSeed() { return seed; }
    public double getFinalTime() { return finalTime; }
    public long getProcessedEvents() { return processedEvents; }
    public Map<String, String> getChainTips() { return chainTips; }
    public Map<String, Integer> getChainHeights() { return chainHeights; }
}
