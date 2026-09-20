package jp.kota.bcasim.tool.fileio;

import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.network.Network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Final snapshots and summaries, separate from streaming event output. */
final class ResultFiles {
    private ResultFiles() { }

    static void write(Path directory, SimulationConfig config, Network network,
                      Map<String, Long> eventCounts, long blockCount) throws IOException {
        Blockchain chain = network.getNodeList().get(0).getBlockchain();
        writeChain(directory, chain, config.getBlockReward());
        writeConfiguration(directory, config, network);
        writeReplayConfiguration(directory, config);
        writeMatrix(directory, "adjacencyMatrix.csv", network.getAdjacencyMatrix());
        writeMatrix(directory, "initialAdjacencyMatrix.csv", config.getAdjacencyMatrix());
        writeMainchain(directory, chain, network);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("seed", config.getSeed());
        metrics.put("simulationTime", config.getSimulationTime());
        metrics.put("eventsByType", eventCounts);
        metrics.put("recordedBlocksIncludingGenesis", blockCount);
        Map<String, Integer> heights = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Node node : network.getNodeList()) {
            heights.put(node.getName(), node.getBlockchain().getHeight());
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("name", node.getName());
            metadata.put("weight", node.getHashrate());
            metadata.put("behavior", node.getBehavior().getClass().getName());
            metadata.put("consensus", node.getConsensus().getClass().getName());
            nodes.add(metadata);
        }
        metrics.put("finalHeights", heights);
        // Order matches the row and column order of the final adjacencyMatrix.csv snapshot.
        metrics.put("finalNodes", nodes);
        metrics.putAll(jp.kota.bcasim.main.SimulationMetrics.collect(network.getNodeList().get(0).getSimulation()));
        try (BufferedWriter writer = FileResultWriter.open(directory.resolve("metrics.json"))) {
            JsonWriter.write(writer, metrics);
            writer.newLine();
        }
    }

    private static void writeChain(Path directory, Blockchain chain, double blockReward) throws IOException {
        String prefix = chain.getChainID() + "_blockchain";
        try (JsonArrayWriter json = new JsonArrayWriter(FileResultWriter.open(directory.resolve(prefix + ".json")));
             BufferedWriter csv = FileResultWriter.open(directory.resolve(prefix + ".csv"))) {
            Deque<Block> queue = new ArrayDeque<>();
            queue.add(chain.getGenesis());
            while (!queue.isEmpty()) {
                Block block = queue.removeFirst();
                json.append(ResultRecords.block(block, blockReward));
                writeCsvRow(csv, block.getHash(), block.getPreviousHash(), String.valueOf(block.getTimestamp()),
                        String.valueOf(block.getHeight()), block.getMiner().getName());
                queue.addAll(block.getNextBlocks());
            }
        }
    }

    private static void writeConfiguration(Path directory, SimulationConfig config, Network network) throws IOException {
        List<Node> nodes = network.getNodeList();
        try (BufferedWriter writer = FileResultWriter.open(directory.resolve("configuration.txt"))) {
            writer.write("simulation configuration");
            writer.newLine();
            setting(writer, "SIMULATION_TIME", config.getSimulationTime());
            setting(writer, "BLOCK_INTERVAL", config.getBlockInterval());
            setting(writer, "CONSENSUS", config.getConsensus());
            setting(writer, "BLOCK_SIZE", config.getBlockSize());
            setting(writer, "BLOCK_REWARD", config.getBlockReward());
            setting(writer, "BLOCK_DELAY", config.getBlockDelay());
            setting(writer, "TRANSACTION_DELAY", config.getTransactionDelay());
            setting(writer, "TRANSACTION_SIZE", config.getTransactionSize());
            setting(writer, "TRANSACTION_RATE", config.getTransactionRate());
            setting(writer, "TRANSACTION_VALUE", config.getTransactionValue());
            setting(writer, "INITIAL_BALANCE", config.getInitialBalance());
            setting(writer, "OBSERVER_NODE", config.getObserverNode());
            setting(writer, "NETWORK_CHANGES", config.toProperties().getProperty("network.changes"));
            setting(writer, "GENERATE_TRANSACTIONS", config.isGenerateTransactions());
            setting(writer, "NUMBER_OF_NODES", config.getNumberOfNodes());
            setting(writer, "FINAL_NUMBER_OF_NODES", nodes.size());
            setting(writer, "SEED", config.getSeed());
            setting(writer, "NODE_STRATEGIES", Arrays.toString(config.getNodeStrategies()));
            writer.write("HASHRATE_LIST:{");
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) writer.write(',');
                writer.write(String.valueOf(nodes.get(i).getHashrate()));
            }
            writer.write('}');
            writer.newLine();
        }
    }

    /** Initial configuration can be loaded directly by --config; no wall-clock timestamp is added. */
    private static void writeReplayConfiguration(Path directory, SimulationConfig config) throws IOException {
        java.util.Properties properties = config.toProperties();
        Map<String, Object> values = new java.util.TreeMap<>();
        for (String key : properties.stringPropertyNames()) values.put(key, properties.getProperty(key));
        try (BufferedWriter writer = FileResultWriter.open(directory.resolve("configuration.properties"))) {
            writer.write("# Initial experiment configuration. Final topology is in adjacencyMatrix.csv.");
            writer.newLine();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String value = entry.getValue().toString().replace("\\", "\\\\").replace("\n", "\\n")
                        .replace("\r", "\\r").replace("\t", "\\t").replace("\f", "\\f").replace(" ", "\\ ");
                writer.write(entry.getKey() + "=" + value);
                writer.newLine();
            }
        }
    }

    private static void setting(BufferedWriter writer, String key, Object value) throws IOException {
        writer.write(key + ":" + value);
        writer.newLine();
    }

    private static void writeMatrix(Path directory, String filename, int[][] matrix) throws IOException {
        try (BufferedWriter writer = FileResultWriter.open(directory.resolve(filename))) {
            for (int[] row : matrix) {
                for (int column = 0; column < row.length; column++) {
                    if (column > 0) writer.write(',');
                    writer.write(String.valueOf(row[column]));
                }
                writer.newLine();
            }
        }
    }

    private static void writeMainchain(Path directory, Blockchain chain, Network network) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Node node : network.getNodeList()) counts.put(node.getName(), 0);
        int total = 0;
        for (Block block = chain.getLatestBlock(); block != null; block = block.getPreviousBlock()) {
            String miner = block.getMiner().getName();
            if (counts.containsKey(miner)) {
                counts.put(miner, counts.get(miner) + 1);
                total++;
            }
        }
        try (BufferedWriter writer = FileResultWriter.open(directory.resolve("Mainchain.txt"))) {
            for (Map.Entry<String, Integer> count : counts.entrySet()) {
                writer.write("node" + count.getKey() + ": " + count.getValue());
                writer.newLine();
            }
            writer.write("合計: " + total);
            writer.newLine();
        }
    }

    private static void writeCsvRow(BufferedWriter writer, String... fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) writer.write(',');
            String field = fields[i] == null ? "" : fields[i];
            if (field.indexOf(',') >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0) {
                writer.write('"');
                writer.write(field.replace("\"", "\"\""));
                writer.write('"');
            } else {
                writer.write(field);
            }
        }
        writer.newLine();
    }
}
