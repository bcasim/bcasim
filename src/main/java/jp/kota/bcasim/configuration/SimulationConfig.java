package jp.kota.bcasim.configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Immutable, validated inputs to one experiment. Array getters return copies. */
public final class SimulationConfig {
    private final double simulationTime, blockInterval, blockSize, blockReward;
    private final double transactionSize, blockDelay, transactionDelay;
    private final long seed;
    private final String consensus;
    private final boolean generateTransactions;
    private final double[] hashrates;
    private final int[][] adjacencyMatrix;
    private final String[] nodeStrategies;

    private SimulationConfig(Builder b) {
        simulationTime = nonnegative("simulation.time", b.simulationTime);
        blockInterval = positive("block.interval", b.blockInterval);
        blockSize = positive("block.size", b.blockSize);
        blockReward = nonnegative("block.reward", b.blockReward);
        transactionSize = positive("transaction.size", b.transactionSize);
        blockDelay = nonnegative("network.blockDelay", b.blockDelay);
        transactionDelay = nonnegative("network.transactionDelay", b.transactionDelay);
        seed = b.seed;
        consensus = b.consensus;
        if (!"PoW".equals(consensus) && !"PoS".equals(consensus)) {
            throw new IllegalArgumentException("consensus must be PoW or PoS");
        }
        generateTransactions = b.generateTransactions;
        hashrates = b.hashrates.clone();
        if (hashrates.length == 0) throw new IllegalArgumentException("At least one node is required");
        boolean active = false;
        for (double rate : hashrates) {
            nonnegative("node weight", rate);
            if (rate > 0) {
                positive("block.interval / node weight", blockInterval / rate);
                active = true;
            }
        }
        if (!active) throw new IllegalArgumentException("At least one node must have positive weight");
        adjacencyMatrix = copy(b.adjacencyMatrix);
        if (adjacencyMatrix.length != hashrates.length) throw new IllegalArgumentException("Matrix and node counts differ");
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            if (adjacencyMatrix[i].length != hashrates.length) throw new IllegalArgumentException("Matrix must be square");
            for (int j = 0; j < adjacencyMatrix[i].length; j++) {
                int value = adjacencyMatrix[i][j];
                if (value != 0 && value != 1) throw new IllegalArgumentException("Matrix values must be 0 or 1");
                if (i == j && value != 0) throw new IllegalArgumentException("Self connections are not supported");
            }
        }
        nodeStrategies = b.nodeStrategies.clone();
        if (nodeStrategies.length != hashrates.length) throw new IllegalArgumentException("Strategy and node counts differ");
        for (String strategy : nodeStrategies) {
            if (strategy == null || strategy.trim().isEmpty()) throw new IllegalArgumentException("Empty node strategy");
        }
    }

    public static SimulationConfig defaults() { return builder().build(); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(this); }

    public static SimulationConfig load(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(reader); }
        Set<String> allowed = new HashSet<>(Arrays.asList("simulation.time", "seed", "consensus", "block.interval",
            "block.size", "block.reward", "transaction.size", "transaction.generate", "network.blockDelay",
            "network.transactionDelay", "nodes.weights", "nodes.strategies", "network.matrix"));
        for (String key : p.stringPropertyNames()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown configuration key: " + key);
        }
        Builder b = builder();
        if (p.containsKey("simulation.time")) b.simulationTime(Double.parseDouble(p.getProperty("simulation.time")));
        if (p.containsKey("seed")) b.seed(Long.parseLong(p.getProperty("seed")));
        if (p.containsKey("consensus")) b.consensus(p.getProperty("consensus").trim());
        if (p.containsKey("block.interval")) b.blockInterval(Double.parseDouble(p.getProperty("block.interval")));
        if (p.containsKey("block.size")) b.blockSize(Double.parseDouble(p.getProperty("block.size")));
        if (p.containsKey("block.reward")) b.blockReward(Double.parseDouble(p.getProperty("block.reward")));
        if (p.containsKey("transaction.size")) b.transactionSize(Double.parseDouble(p.getProperty("transaction.size")));
        if (p.containsKey("transaction.generate")) {
            String value = p.getProperty("transaction.generate").trim();
            if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("transaction.generate must be true or false");
            b.generateTransactions(Boolean.parseBoolean(value));
        }
        if (p.containsKey("network.blockDelay")) b.blockDelay(Double.parseDouble(p.getProperty("network.blockDelay")));
        if (p.containsKey("network.transactionDelay")) b.transactionDelay(Double.parseDouble(p.getProperty("network.transactionDelay")));
        if (p.containsKey("nodes.weights")) {
            String[] values = split(p.getProperty("nodes.weights"));
            double[] weights = new double[values.length];
            for (int i = 0; i < weights.length; i++) weights[i] = Double.parseDouble(values[i]);
            b.hashrates(weights);
        }
        if (p.containsKey("nodes.strategies")) b.nodeStrategies(split(p.getProperty("nodes.strategies")));
        if (p.containsKey("network.matrix")) {
            String[] rows = p.getProperty("network.matrix").split(";", -1);
            int[][] matrix = new int[rows.length][];
            for (int i = 0; i < rows.length; i++) {
                String[] columns = split(rows[i]);
                matrix[i] = new int[columns.length];
                for (int j = 0; j < columns.length; j++) matrix[i][j] = Integer.parseInt(columns[j]);
            }
            b.adjacencyMatrix(matrix);
        }
        return b.build();
    }

    private static String[] split(String value) {
        String[] result = value.split(",", -1);
        for (int i = 0; i < result.length; i++) result[i] = result[i].trim();
        return result;
    }
    private static double nonnegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(name + " must be finite and nonnegative");
        return value;
    }
    private static double positive(String name, double value) {
        nonnegative(name, value);
        if (value == 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
    private static int[][] copy(int[][] value) {
        int[][] result = new int[value.length][];
        for (int i = 0; i < value.length; i++) result[i] = value[i].clone();
        return result;
    }
    public double getSimulationTime() { return simulationTime; }
    public double getBlockInterval() { return blockInterval; }
    public double getBlockSize() { return blockSize; }
    public double getBlockReward() { return blockReward; }
    public double getTransactionSize() { return transactionSize; }
    public double getBlockDelay() { return blockDelay; }
    public double getTransactionDelay() { return transactionDelay; }
    public long getSeed() { return seed; }
    public String getConsensus() { return consensus; }
    public boolean isGenerateTransactions() { return generateTransactions; }
    public int getNumberOfNodes() { return hashrates.length; }
    public double[] getHashrates() { return hashrates.clone(); }
    public int[][] getAdjacencyMatrix() { return copy(adjacencyMatrix); }
    public String[] getNodeStrategies() { return nodeStrategies.clone(); }

    public static final class Builder {
        private double simulationTime = 100000, blockInterval = 10, blockSize = 8, blockReward = 10;
        private double transactionSize = 1, blockDelay = 0, transactionDelay = 15;
        private long seed = 1;
        private String consensus = "PoW";
        private boolean generateTransactions = true;
        private double[] hashrates = {0.48, 0.52};
        private int[][] adjacencyMatrix = {{0, 1}, {1, 0}};
        private String[] nodeStrategies = {"selfish", "honest"};
        private Builder() {}
        private Builder(SimulationConfig c) {
            simulationTime = c.simulationTime; blockInterval = c.blockInterval; blockSize = c.blockSize;
            blockReward = c.blockReward; transactionSize = c.transactionSize; blockDelay = c.blockDelay;
            transactionDelay = c.transactionDelay; seed = c.seed; consensus = c.consensus;
            generateTransactions = c.generateTransactions; hashrates = c.getHashrates();
            adjacencyMatrix = c.getAdjacencyMatrix(); nodeStrategies = c.getNodeStrategies();
        }
        public Builder simulationTime(double v) { simulationTime = v; return this; }
        public Builder blockInterval(double v) { blockInterval = v; return this; }
        public Builder blockSize(double v) { blockSize = v; return this; }
        public Builder blockReward(double v) { blockReward = v; return this; }
        public Builder transactionSize(double v) { transactionSize = v; return this; }
        public Builder blockDelay(double v) { blockDelay = v; return this; }
        public Builder transactionDelay(double v) { transactionDelay = v; return this; }
        public Builder seed(long v) { seed = v; return this; }
        public Builder consensus(String v) { consensus = v; return this; }
        public Builder generateTransactions(boolean v) { generateTransactions = v; return this; }
        public Builder hashrates(double... v) { hashrates = v.clone(); return this; }
        public Builder adjacencyMatrix(int[][] v) { adjacencyMatrix = copy(v); return this; }
        public Builder nodeStrategies(String... v) { nodeStrategies = v.clone(); return this; }
        public SimulationConfig build() { return new SimulationConfig(this); }
    }
}
