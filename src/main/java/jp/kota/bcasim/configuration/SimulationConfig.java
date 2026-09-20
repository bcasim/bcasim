package jp.kota.bcasim.configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Immutable, validated inputs to one experiment. Array getters return copies. */
public final class SimulationConfig {
    private final double simulationTime, blockInterval, blockSize, blockReward;
    private final double transactionSize, blockDelay, transactionDelay;
    private final long seed;
    private final double transactionRate, initialBalance;
    private final int transactionValue, observerNode;
    private final List<NetworkChangeSpec> networkChanges;
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
        transactionRate = nonnegative("transaction.rate", b.transactionRate);
        initialBalance = nonnegative("transaction.initialBalance", b.initialBalance);
        transactionValue = b.transactionValue;
        if (transactionValue <= 0) throw new IllegalArgumentException("transaction.value must be positive");
        observerNode = b.observerNode;
        networkChanges = Collections.unmodifiableList(new ArrayList<>(b.networkChanges));
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
        if (observerNode < 0 || observerNode >= hashrates.length) throw new IllegalArgumentException("observer.node is outside the network");
        if (generateTransactions && transactionRate > 0 && hashrates.length < 2) throw new IllegalArgumentException("Transaction generation requires at least two nodes");
        for (NetworkChangeSpec change : networkChanges) change.validate(hashrates.length, simulationTime);
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
            "network.transactionDelay", "nodes.weights", "nodes.strategies", "network.matrix",
            "transaction.rate", "transaction.value", "transaction.initialBalance", "observer.node", "network.changes"));
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
        if (p.containsKey("transaction.rate")) b.transactionRate(Double.parseDouble(p.getProperty("transaction.rate")));
        if (p.containsKey("transaction.value")) b.transactionValue(Integer.parseInt(p.getProperty("transaction.value")));
        if (p.containsKey("transaction.initialBalance")) b.initialBalance(Double.parseDouble(p.getProperty("transaction.initialBalance")));
        if (p.containsKey("observer.node")) b.observerNode(Integer.parseInt(p.getProperty("observer.node")));
        if (p.containsKey("network.changes")) b.networkChanges(NetworkChangeSpec.parse(p.getProperty("network.changes")));
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
    public double getTransactionRate() { return transactionRate; }
    public int getTransactionValue() { return transactionValue; }
    public double getInitialBalance() { return initialBalance; }
    public int getObserverNode() { return observerNode; }
    public List<NetworkChangeSpec> getNetworkChanges() { return networkChanges; }
    /** Complete initial inputs; callers may sort keys for deterministic serialization. */
    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("seed", String.valueOf(seed));
        p.setProperty("simulation.time", String.valueOf(simulationTime));
        p.setProperty("consensus", consensus);
        p.setProperty("block.interval", String.valueOf(blockInterval));
        p.setProperty("block.size", String.valueOf(blockSize));
        p.setProperty("block.reward", String.valueOf(blockReward));
        p.setProperty("transaction.size", String.valueOf(transactionSize));
        p.setProperty("transaction.generate", String.valueOf(generateTransactions));
        p.setProperty("transaction.rate", String.valueOf(transactionRate));
        p.setProperty("transaction.value", String.valueOf(transactionValue));
        p.setProperty("transaction.initialBalance", String.valueOf(initialBalance));
        p.setProperty("observer.node", String.valueOf(observerNode));
        p.setProperty("network.blockDelay", String.valueOf(blockDelay));
        p.setProperty("network.transactionDelay", String.valueOf(transactionDelay));
        StringBuilder weights = new StringBuilder(), matrix = new StringBuilder(), changes = new StringBuilder();
        for (double rate : hashrates) { if (weights.length() > 0) weights.append(','); weights.append(rate); }
        for (int[] row : adjacencyMatrix) {
            if (matrix.length() > 0) matrix.append(';');
            for (int j = 0; j < row.length; j++) { if (j > 0) matrix.append(','); matrix.append(row[j]); }
        }
        for (NetworkChangeSpec change : networkChanges) { if (changes.length() > 0) changes.append(';'); changes.append(change); }
        p.setProperty("nodes.weights", weights.toString());
        p.setProperty("nodes.strategies", String.join(",", nodeStrategies));
        p.setProperty("network.matrix", matrix.toString());
        p.setProperty("network.changes", changes.toString());
        return p;
    }
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
        private double transactionRate = 0, initialBalance = 1000;
        private int transactionValue = 1, observerNode = 0;
        private List<NetworkChangeSpec> networkChanges = new ArrayList<>();
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
            transactionRate = c.transactionRate; initialBalance = c.initialBalance; transactionValue = c.transactionValue;
            observerNode = c.observerNode; networkChanges = new ArrayList<>(c.networkChanges);
        }
        public Builder simulationTime(double v) { simulationTime = v; return this; }
        public Builder blockInterval(double v) { blockInterval = v; return this; }
        public Builder blockSize(double v) { blockSize = v; return this; }
        public Builder blockReward(double v) { blockReward = v; return this; }
        public Builder transactionSize(double v) { transactionSize = v; return this; }
        public Builder blockDelay(double v) { blockDelay = v; return this; }
        public Builder transactionDelay(double v) { transactionDelay = v; return this; }
        public Builder transactionRate(double v) { transactionRate = v; return this; }
        public Builder transactionValue(int v) { transactionValue = v; return this; }
        public Builder initialBalance(double v) { initialBalance = v; return this; }
        public Builder observerNode(int v) { observerNode = v; return this; }
        public Builder networkChanges(List<NetworkChangeSpec> v) { networkChanges = new ArrayList<>(v); return this; }
        public Builder seed(long v) { seed = v; return this; }
        public Builder consensus(String v) { consensus = v; return this; }
        public Builder generateTransactions(boolean v) { generateTransactions = v; return this; }
        public Builder hashrates(double... v) { hashrates = v.clone(); return this; }
        public Builder adjacencyMatrix(int[][] v) { adjacencyMatrix = copy(v); return this; }
        public Builder nodeStrategies(String... v) { nodeStrategies = v.clone(); return this; }
        public SimulationConfig build() { return new SimulationConfig(this); }
    }
}
