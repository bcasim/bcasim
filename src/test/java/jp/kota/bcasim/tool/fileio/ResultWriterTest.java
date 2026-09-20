package jp.kota.bcasim.tool.fileio;

import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.Simulation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ResultWriterTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void jsonEscapesTextAndSeparatesNonemptyArrays() throws Exception {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("text", "quote\" slash\\ tab\t line\n\r\b\f\u0001 日本語 😀");
        record.put("transaction", Arrays.asList("a", "b"));
        StringWriter writer = new StringWriter();
        JsonWriter.write(writer, record);
        assertEquals("{\"text\":\"quote\\\" slash\\\\ tab\\t line\\n\\r\\b\\f\\u0001 日本語 \\ud83d\\ude00\","
                + "\"transaction\":[\"a\",\"b\"]}", writer.toString());
    }

    @Test
    public void blockRecordsKeepVisualizerFieldsAndTransactionHashes() throws Exception {
        ArrayList<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction("0", "1", 2, "first"));
        transactions.add(new Transaction("1", "0", 3, "second"));
        Block block = new Block("quoted\"hash", "parent", 12.5, 7, null, transactions);
        block.setReceiveBlockTime(13.0);
        Map<String, Object> record = ResultRecords.block(block, 10);
        assertEquals(Arrays.asList("hash", "previousHash", "timestamp", "receiveTime", "height", "miner", "reward", "transaction"),
                new ArrayList<>(record.keySet()));
        assertEquals("12.5", record.get("timestamp"));
        assertEquals("13.0", record.get("receiveTime"));
        assertEquals("7", record.get("height"));
        assertEquals(Arrays.asList(transactions.get(0).getTransactionHash(), transactions.get(1).getTransactionHash()),
                record.get("transaction"));
        StringWriter writer = new StringWriter();
        JsonWriter.write(writer, record);
        assertTrue(writer.toString().contains("\"hash\":\"quoted\\\"hash\""));
        assertFalse(writer.toString().contains(",]"));
    }

    @Test
    public void repeatedSeedProducesIdenticalCompleteOutputFiles() throws Exception {
        SimulationConfig config = SimulationConfig.builder().seed(1234).simulationTime(80)
                .nodeStrategies("honest", "honest").build();
        Path first = temporary.newFolder("first").toPath();
        Path second = temporary.newFolder("second").toPath();
        new Simulation(config, new FileResultWriter(first)).run();
        new Simulation(config, new FileResultWriter(second)).run();
        for (String file : Arrays.asList("block.json", "event.json", "0_blockchain.json", "0_blockchain.csv",
                "adjacencyMatrix.csv", "configuration.txt", "configuration.properties", "Mainchain.txt", "attackLog.txt", "metrics.json")) {
            assertTrue("Missing " + file, Files.isRegularFile(first.resolve(file)));
            assertArrayEquals("Different " + file, Files.readAllBytes(first.resolve(file)), Files.readAllBytes(second.resolve(file)));
        }
        for (String file : Arrays.asList("block.json", "event.json", "0_blockchain.json")) {
            String json = text(first.resolve(file)).trim();
            assertTrue(json.startsWith("["));
            assertTrue(json.endsWith("]"));
            assertFalse(json.matches("(?s).*,\\s*\\].*"));
        }
        Matcher eventTimes = Pattern.compile("\"time\":\"([^\"]+)\"").matcher(text(first.resolve("event.json")));
        int events = 0;
        while (eventTimes.find()) {
            assertTrue("Event beyond simulation horizon", Double.parseDouble(eventTimes.group(1)) <= config.getSimulationTime());
            events++;
        }
        assertTrue("Expected recorded events", events > 0);
        assertTrue(text(first.resolve("configuration.txt")).contains("SEED:1234"));
        assertTrue(text(first.resolve("Mainchain.txt")).contains("node0: "));
        assertTrue(text(first.resolve("Mainchain.txt")).contains("node1: "));
    }

    @Test
    public void sixNodeOutputCountsEveryMinerAndCapturesReplayConfiguration() throws Exception {
        int[][] matrix = new int[6][6];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) matrix[i][j] = i == j ? 0 : 1;
        }
        SimulationConfig config = SimulationConfig.builder().seed(72).simulationTime(12).blockInterval(1)
                .blockSize(7).blockReward(3.25).transactionSize(0.5).generateTransactions(false)
                .blockDelay(0.01).transactionDelay(0.25).hashrates(0, 0, 0, 0, 0, 1)
                .nodeStrategies("honest", "honest", "honest", "honest", "honest", "honest")
                .adjacencyMatrix(matrix).build();
        Path directory = temporary.newFolder("six").toPath();
        Simulation simulation = new Simulation(config, new FileResultWriter(directory));
        simulation.run();
        int height = simulation.getNetwork().getNodeList().get(0).getBlockchain().getHeight();
        assertTrue("Expected blocks from node 5", height > 0);
        String counts = text(directory.resolve("Mainchain.txt"));
        for (int i = 0; i < 5; i++) assertTrue(counts.contains("node" + i + ": 0\n"));
        assertTrue(counts.contains("node5: " + height + "\n"));
        assertTrue(counts.contains("合計: " + height + "\n"));
        String[] rows = text(directory.resolve("adjacencyMatrix.csv")).trim().split("\\R");
        assertEquals(6, rows.length);
        for (String row : rows) assertEquals(6, row.split(",").length);
        assertTrue(text(directory.resolve("metrics.json")).contains("\"name\":\"5\""));

        SimulationConfig replay = SimulationConfig.load(directory.resolve("configuration.properties"));
        assertEquals(config.getSeed(), replay.getSeed());
        assertEquals(config.getSimulationTime(), replay.getSimulationTime(), 0);
        assertEquals(config.getConsensus(), replay.getConsensus());
        assertEquals(config.getBlockInterval(), replay.getBlockInterval(), 0);
        assertEquals(config.getBlockSize(), replay.getBlockSize(), 0);
        assertEquals(config.getBlockReward(), replay.getBlockReward(), 0);
        assertEquals(config.getTransactionSize(), replay.getTransactionSize(), 0);
        assertEquals(config.isGenerateTransactions(), replay.isGenerateTransactions());
        assertEquals(config.getBlockDelay(), replay.getBlockDelay(), 0);
        assertEquals(config.getTransactionDelay(), replay.getTransactionDelay(), 0);
        assertArrayEquals(config.getHashrates(), replay.getHashrates(), 0);
        assertArrayEquals(config.getNodeStrategies(), replay.getNodeStrategies());
        assertTrue(Arrays.deepEquals(config.getAdjacencyMatrix(), replay.getAdjacencyMatrix()));
    }

    @Test
    public void existingResultsAreNeverOverwrittenOrDeleted() throws Exception {
        Path directory = temporary.newFolder("existing").toPath();
        Path previous = directory.resolve("block.json");
        Files.write(previous, "previous result".getBytes(StandardCharsets.UTF_8));
        try {
            new Simulation(SimulationConfig.builder().simulationTime(0).build(), new FileResultWriter(directory)).run();
            fail("Expected existing output directory to be refused");
        } catch (UncheckedIOException expected) {
            assertTrue(expected.getCause().getMessage().contains("not empty"));
        }
        assertEquals("previous result", text(previous));
        assertFalse(Files.exists(directory.resolve("event.json")));
    }

    @Test
    public void closeFinalizesJsonAfterInterruptedRecording() throws Exception {
        Path directory = temporary.newFolder("interrupted").toPath();
        FileResultWriter files = new FileResultWriter(directory);
        ResultWriter interrupted = new ResultWriter() {
            @Override public void start(SimulationConfig config, jp.kota.bcasim.network.Network network) {
                files.start(config, network);
                throw new IllegalStateException("stop after start");
            }
            @Override public void close() { files.close(); }
        };
        try {
            new Simulation(SimulationConfig.defaults(), interrupted).run();
            fail("Expected interrupted simulation");
        } catch (IllegalStateException expected) {
            assertEquals("stop after start", expected.getMessage());
        }
        assertEquals("[]", text(directory.resolve("event.json")).replaceAll("\\s", ""));
        assertTrue(text(directory.resolve("block.json")).trim().endsWith("]"));
        files.close();
    }

    private static String text(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
