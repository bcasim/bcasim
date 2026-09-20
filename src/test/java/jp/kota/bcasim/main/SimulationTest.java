package jp.kota.bcasim.main;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.Test;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.*;
import jp.kota.bcasim.main.event.*;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.main.node.behavior.*;
import jp.kota.bcasim.main.node.consensus.*;
import jp.kota.bcasim.network.NetworkGenerator;
import jp.kota.bcasim.tool.fileio.ResultWriter;

public class SimulationTest {
    private SimulationConfig config(long seed) { return SimulationConfig.builder().seed(seed).simulationTime(300).build(); }
    private static final class Trace implements ResultWriter {
        final List<String> events = new ArrayList<>();
        public void recordEvent(Event event) {
            String block = event instanceof FoundBlock ? ((FoundBlock)event).getBlock().getHash() :
                event instanceof ReceiveBlock ? ((ReceiveBlock)event).getBlock().getHash() : "";
            events.add(event.getEventID() + ":" + event.getEventTime() + ":" + event.getNode().getName() + ":" + event.getEventType() + ":" + block);
        }
    }
    @Test public void sameSeedReplaysAndDifferentSeedChangesTheTrace() {
        Trace first = new Trace(), repeat = new Trace(), different = new Trace();
        new Simulation(config(42), first).run();
        new Simulation(config(42), repeat).run();
        new Simulation(config(43), different).run();
        assertEquals(first.events, repeat.events);
        assertNotEquals(first.events, different.events);
    }
    @Test public void concurrentExperimentsHaveIndependentClocksQueuesAndRandomness() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<List<String>>> futures = new ArrayList<>();
            for (int i = 0; i < 12; i++) futures.add(pool.submit(() -> {
                Trace trace = new Trace(); new Simulation(config(7), trace).run(); return trace.events;
            }));
            List<String> expected = futures.get(0).get();
            for (Future<List<String>> future : futures) assertEquals(expected, future.get());
        } finally { pool.shutdownNow(); }
    }
    @Test public void simulationIsSingleUseAndItsSummaryIsImmutable() {
        Simulation simulation = new Simulation(config(1));
        SimulationResult result = simulation.run();
        assertTrue(result.getProcessedEvents() > 2);
        assertTrue(result.getFinalTime() <= 300);
        assertThrows(IllegalStateException.class, simulation::run);
        assertThrows(UnsupportedOperationException.class, () -> result.getChainTips().clear());
    }
    @Test public void timestampBoundaryIsInclusiveAndBeyondHorizonIsNotRecorded() {
        SimulationConfig config = config(1).toBuilder().simulationTime(10).consensus("PoS")
            .hashrates(1).nodeStrategies("honest").adjacencyMatrix(new int[][]{{0}}).build();
        Trace trace = new Trace();
        SimulationResult result = new Simulation(config, trace).run();
        assertEquals(2, trace.events.size());
        assertEquals(10, result.getFinalTime(), 0);
        assertEquals(Integer.valueOf(1), result.getChainHeights().get("0"));
    }
    @Test public void zeroWeightNodesReceiveBlocksWithoutMining() {
        Simulation simulation = new Simulation(config(1).toBuilder().hashrates(0, 1)
            .nodeStrategies("honest", "honest").build());
        SimulationResult result = simulation.run();
        assertEquals(result.getChainTips().get("0"), result.getChainTips().get("1"));
        assertNull(simulation.getScheduler().getFoundEvent(simulation.getNetwork().getNodeList().get(0)));
    }
    @Test public void consensusBehaviorAndForkChoiceAreInjectable() {
        List<String> calls = new ArrayList<>();
        SimulationConfig config = config(1).toBuilder().nodeStrategies("custom", "custom").build();
        Simulation simulation = new Simulation(config, ResultWriter.NOOP,
            (node, weight) -> { calls.add("consensus:" + node.getName()); return new PoS(node, weight); },
            key -> new NodeBehavior() { public void initialize(Node node) { calls.add("init:" + node.getName()); } },
            (chain, current, candidate) -> current);
        simulation.run();
        assertEquals(java.util.Arrays.asList("consensus:0", "consensus:1", "init:0", "init:1"), calls);
    }
    @Test public void forkChoiceKeepsFirstForeignTieButPrefersOwnBlockAndLongerChain() {
        Simulation simulation = new Simulation(config(1));
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block genesis = a.getBlockchain().getGenesis();
        Block first = new Block("first", genesis, 1, b, new ArrayList<Transaction>());
        Block foreign = new Block("foreign", genesis, 2, b, new ArrayList<Transaction>());
        Block own = new Block("own", genesis, 3, a, new ArrayList<Transaction>());
        Block longer = new Block("longer", first, 4, b, new ArrayList<Transaction>());
        ForkChoice choice = new LongestChain();
        assertSame(first, choice.select("0", first, foreign));
        assertSame(own, choice.select("0", first, own));
        assertSame(longer, choice.select("0", own, longer));
    }
    @Test public void cloningPreservesPropagationButNotChainLinks() {
        Simulation simulation = new Simulation(config(1));
        Node node = simulation.getNetwork().getNodeList().get(0);
        Block original = node.generateNewBlock(0), clone = Block.cloneBlock(original);
        assertSame(original.getTransmittedNodes(), clone.getTransmittedNodes());
        assertSame(original.getBalanceList(), clone.getBalanceList());
        assertNotSame(original.getNextBlocks(), clone.getNextBlocks());
        clone.addTransmittedNodes(node);
        assertTrue(original.verifyNode(node));
        assertNull(clone.getPreviousBlock());
    }
    @Test public void invalidAndCrossExperimentEventsFailWithoutTerminatingProcess() {
        Simulation a = new Simulation(config(1)), b = new Simulation(config(2));
        Node node = a.getNetwork().getNodeList().get(0);
        assertThrows(IllegalArgumentException.class, () -> a.getScheduler().addNewEvent(new InitNode(Double.NaN, node)));
        assertThrows(IllegalArgumentException.class, () -> a.getScheduler().addNewEvent(new InitNode(-1, node)));
        assertThrows(IllegalArgumentException.class, () -> a.getScheduler().addNewEvent(new InitNode(0, b.getNetwork().getNodeList().get(0))));
        Block block = b.getNetwork().getNodeList().get(0).generateNewBlock();
        assertThrows(IllegalArgumentException.class, () -> node.addNewBlock(block));
    }
    @Test public void missingParentIsReportedAsAnError() {
        Simulation simulation = new Simulation(config(1));
        Node node = simulation.getNetwork().getNodeList().get(0);
        Block invalid = new Block("x", "absent", 1, 1, node, new ArrayList<Transaction>());
        assertThrows(IllegalArgumentException.class, () -> node.addNewBlock(invalid));
    }
    @Test public void transactionIdentityAndBlockCapacityAreDeterministic() {
        SimulationConfig config = config(1).toBuilder().blockSize(2).transactionSize(1).build();
        Simulation a = new Simulation(config), b = new Simulation(config);
        Node node = a.getNetwork().getNodeList().get(0);
        for (int i = 0; i < 3; i++) {
            Transaction transaction = a.createTransaction("0", "1", i);
            assertEquals(transaction.getTransactionHash(), b.createTransaction("0", "1", i).getTransactionHash());
            node.addTransaction(transaction);
        }
        assertEquals(2, node.generateNewBlock().getTransactionList().size());
        assertEquals(2, node.getTransactionPool().getTransactions().size());
        assertEquals("supplied", new Transaction("0", "1", 1, "supplied").getTransactionHash());
    }
    @Test public void cacheFindsHeightAndRetainsItsFullCapacity() {
        BlockCache cache = new BlockCache();
        for (int i = 0; i < 1001; i++) cache.addBlock(new Block("b"+i, "none", i, i, null, null));
        assertNull(cache.serchHash("b0"));
        assertNotNull(cache.serchHash("b1"));
        assertEquals("b1000", cache.serchHeight(1000).getHash());
    }
    @Test public void topologyFactoriesAndNetworkSnapshotsAreIndependent() {
        assertArrayEquals(new int[][]{{0}}, NetworkGenerator.ring(1));
        assertArrayEquals(new int[][]{{0,1,0},{1,0,1},{0,1,0}}, NetworkGenerator.line(3));
        Simulation simulation = new Simulation(config(1));
        simulation.getNetwork().getNodeList().clear();
        simulation.getNetwork().getAdjacencyMatrix()[0][1] = 0;
        assertEquals(2, simulation.getNetwork().getNodeList().size());
        assertEquals(1, simulation.getNetwork().getAdjacencyNode("0").size());
    }
    @Test public void liveMiningWeightIsUsedByNodeAndReporting() {
        Simulation simulation = new Simulation(config(1).toBuilder().nodeStrategies("honest", "honest").build());
        Node node = simulation.getNetwork().getNodeList().get(0);
        PoW consensus = (PoW)node.getConsensus();
        consensus.setHashrate(0);
        assertEquals(0, node.getHashrate(), 0);
        assertFalse(node.canMine());
        simulation.run();
        assertNull(simulation.getScheduler().getFoundEvent(node));
    }
    @Test public void connectedSixNodeNetworksRunWithPropagationDelay() {
        for (int[][] topology : new int[][][]{NetworkGenerator.ring(6), NetworkGenerator.line(6), NetworkGenerator.fullyConnected(6)}) {
            SimulationConfig config = config(1).toBuilder().simulationTime(500).blockDelay(0.5)
                .hashrates(0.15, 0.15, 0.15, 0.15, 0.2, 0.2)
                .nodeStrategies("selfish", "honest", "honest", "honest", "honest", "honest")
                .adjacencyMatrix(topology).build();
            SimulationResult first = new Simulation(config).run();
            SimulationResult replay = new Simulation(config).run();
            assertEquals(first.getChainTips(), replay.getChainTips());
            assertTrue(first.getProcessedEvents() > 6);
        }
    }
}
