package jp.kota.bcasim.main;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import jp.kota.bcasim.configuration.*;
import jp.kota.bcasim.main.event.*;
import jp.kota.bcasim.tool.fileio.*;

public class SimulationFeatureTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private SimulationConfig deterministic() {
        return SimulationConfig.builder().consensus("PoS").hashrates(1, 0)
            .nodeStrategies("honest", "honest").blockInterval(10).blockDelay(2).simulationTime(35).build();
    }
    @Test public void reconnectionFetchesPublicAncestorsAndMeasuresObservedDelay() {
        SimulationConfig config = deterministic().toBuilder().networkChanges(NetworkChangeSpec.parse(
            "5:disconnect:0:1;25:connect:0:1")).build();
        List<Double> receipts = new ArrayList<>();
        Simulation simulation = new Simulation(config, new ResultWriter() {
            public void recordEvent(Event event) {
                if (event instanceof ReceiveBlock && event.getNode().getName().equals("1")) receipts.add(event.getEventTime());
            }
        });
        SimulationResult result = simulation.run();
        assertEquals(Arrays.asList(27.0, 27.0, 32.0), receipts);
        assertEquals(result.getChainTips().get("0"), result.getChainTips().get("1"));
        Map<String,Object> metrics = result.getMetrics();
        assertEquals(3, metrics.get("acceptedBlocks"));
        assertEquals(3, metrics.get("totalPublishedBlocks"));
        assertEquals(0.0, (Double) metrics.get("staleFraction"), 0);
        assertEquals(1.0, (Double) metrics.get("attackerRevenueShare"), 0);
        assertEquals(3L, metrics.get("remoteBlockReceipts"));
        assertEquals(26.0 / 3, (Double) metrics.get("meanPropagationDelay"), 1e-12);
        assertEquals(1030.0, ((Map<?,?>)metrics.get("canonicalBalances")).get("0"));
        assertThrows(UnsupportedOperationException.class, () -> metrics.clear());
    }
    @Test public void disconnectPreservesPacketsAlreadyInFlight() {
        SimulationConfig config = deterministic().toBuilder().simulationTime(16).blockDelay(5)
            .networkChanges(NetworkChangeSpec.parse("12:disconnect:0:1")).build();
        Simulation simulation = new Simulation(config);
        SimulationResult result = simulation.run();
        assertEquals(Integer.valueOf(1), result.getChainHeights().get("1"));
        assertEquals(0, simulation.getNetwork().getAdjacencyMatrix()[0][1]);
        assertEquals(5.0, (Double) result.getMetrics().get("meanPropagationDelay"), 0);
    }
    @Test public void emptyDenominatorsRemainUndefinedAndObserverControlsCanonicalMetrics() {
        Map<String,Object> empty = new Simulation(deterministic().toBuilder().simulationTime(0).build()).run().getMetrics();
        assertEquals(0, empty.get("acceptedBlocks"));
        for (String metric : Arrays.asList("staleFraction", "attackerRevenueShare", "meanPropagationDelay", "attackSuccessRate"))
            assertNull(metric, empty.get(metric));
        SimulationConfig isolated = deterministic().toBuilder().observerNode(1)
            .adjacencyMatrix(new int[][]{{0,0},{0,0}}).build();
        Map<String,Object> metrics = new Simulation(isolated).run().getMetrics();
        assertEquals(0, metrics.get("acceptedBlocks"));
        assertEquals(3, metrics.get("totalPublishedBlocks"));
        assertEquals(1.0, (Double)metrics.get("staleFraction"), 0);
        assertNull(metrics.get("attackerRevenueShare"));
    }
    @Test public void combinedFeaturesReplayEveryFileAndExportInitialTopology() throws Exception {
        SimulationConfig config = deterministic().toBuilder().transactionRate(2).transactionDelay(0.1)
            .transactionValue(3).initialBalance(20).observerNode(1)
            .networkChanges(NetworkChangeSpec.parse("5:disconnect:0:1;25:connect:0:1;34:disconnect:1:0")).build();
        Path original = temporary.newFolder("original").toPath(), replay = temporary.newFolder("replay").toPath();
        new Simulation(config, new FileResultWriter(original)).run();
        SimulationConfig loaded = SimulationConfig.load(original.resolve("configuration.properties"));
        assertEquals(config.toProperties(), loaded.toProperties());
        new Simulation(loaded, new FileResultWriter(replay)).run();
        try (java.util.stream.Stream<Path> files = Files.list(original)) {
            for (Path file : (Iterable<Path>)files::iterator)
                assertArrayEquals(file.toString(), Files.readAllBytes(file), Files.readAllBytes(replay.resolve(file.getFileName())));
        }
        assertEquals("0,1\n1,0\n", text(original.resolve("initialAdjacencyMatrix.csv")));
        assertEquals("0,1\n0,0\n", text(original.resolve("adjacencyMatrix.csv")));
        String events = text(original.resolve("event.json"));
        assertTrue(events.contains("\"type\":\"NetworkChange\""));
        assertTrue(events.contains("\"action\":\"disconnect\""));
        assertTrue(events.contains("\"accountFrom\":"));
        assertTrue(events.contains("\"nonce\":"));
    }
    @Test public void invalidWorkloadAndTimelineInputsFailBeforeRunning() {
        assertThrows(IllegalArgumentException.class, () -> deterministic().toBuilder().transactionRate(-1).build());
        assertThrows(IllegalArgumentException.class, () -> deterministic().toBuilder().initialBalance(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> deterministic().toBuilder().transactionValue(0).build());
        assertThrows(IllegalArgumentException.class, () -> deterministic().toBuilder().observerNode(2).build());
        for (String value : Arrays.asList("1:typo:0:1", "1:connect:0:0", "1:connect:0:2", "36:connect:0:1", "NaN:connect:0:1", "1:connect:0:1;"))
            assertThrows(value, IllegalArgumentException.class, () -> deterministic().toBuilder().networkChanges(NetworkChangeSpec.parse(value)).build());
    }
    @Test public void interruptedRunsCloseWriterWithoutPublishingCompleteSnapshots() {
        boolean[] finished = {false}, closed = {false};
        Simulation simulation = new Simulation(deterministic(), new ResultWriter() {
            public void finish(jp.kota.bcasim.network.Network network) { finished[0] = true; }
            public void close() { closed[0] = true; }
        });
        try {
            Thread.currentThread().interrupt();
            assertThrows(java.util.concurrent.CancellationException.class, simulation::run);
            assertFalse(finished[0]); assertTrue(closed[0]);
            assertEquals(0, simulation.getScheduler().getProcessedEventCount());
        } finally { Thread.interrupted(); }
    }
    private static String text(Path file) throws Exception { return new String(Files.readAllBytes(file), StandardCharsets.UTF_8); }
}
