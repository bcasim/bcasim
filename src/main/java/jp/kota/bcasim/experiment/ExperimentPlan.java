package jp.kota.bcasim.experiment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.main.Simulation;

/** Fully validated Cartesian product of conditions and independent random seeds. */
public final class ExperimentPlan {
    private final List<Run> runs;
    private final int threads;
    private final String fingerprint;

    private ExperimentPlan(List<Run> runs, int threads) {
        this.runs = Collections.unmodifiableList(runs);
        this.threads = threads;
        StringBuilder canonical = new StringBuilder("bcasim-batch-format=1\n");
        for (Run run : runs) canonical.append(run.id).append('=').append(run.fingerprint).append('\n');
        fingerprint = ExperimentFiles.digest(canonical.toString());
    }

    public static ExperimentPlan load(Path file) throws IOException {
        Properties p = ExperimentFiles.read(file);
        Set<String> allowed = new HashSet<>(Arrays.asList("base.config", "seeds", "sweep.attackerShare",
            "sweep.blockDelay", "sweep.strategy", "threads"));
        for (String key : p.stringPropertyNames())
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown batch key: " + key);
        if (!p.containsKey("base.config") || p.getProperty("base.config").trim().isEmpty())
            throw new IllegalArgumentException("Batch requires base.config");
        Path baseFile = file.toAbsolutePath().normalize().getParent().resolve(p.getProperty("base.config").trim()).normalize();
        SimulationConfig base = SimulationConfig.load(baseFile);
        String[] seedStrings = split(p.getProperty("seeds", Long.toString(base.getSeed())));
        List<Long> seeds = new ArrayList<>();
        for (String text : seedStrings) {
            long seed = Long.parseLong(text);
            if (seeds.contains(seed)) throw new IllegalArgumentException("Seeds must be unique: " + seed);
            seeds.add(seed);
        }
        double[] weights = base.getHashrates();
        double total = 0;
        for (double weight : weights) total += weight;
        if (!Double.isFinite(total)) throw new IllegalArgumentException("Total node weight must be finite for batch comparisons");
        double[] shares = doubles(p.getProperty("sweep.attackerShare", Double.toString(weights[0] / total)), "attacker share");
        double[] delays = doubles(p.getProperty("sweep.blockDelay", Double.toString(base.getBlockDelay())), "block delay");
        String[] strategies = split(p.getProperty("sweep.strategy", base.getNodeStrategies()[0]));
        if (new HashSet<>(Arrays.asList(strategies)).size() != strategies.length)
            throw new IllegalArgumentException("Strategies must be unique");
        int threads = Integer.parseInt(p.getProperty("threads", "1").trim());
        validateThreads(threads);
        long count = (long) seeds.size() * shares.length * delays.length * strategies.length;
        if (count > 10000) throw new IllegalArgumentException("A batch is limited to 10000 runs; split this plan into smaller batches");
        List<Run> runs = new ArrayList<>();
        for (String strategy : strategies) for (double share : shares) for (double delay : delays) {
            if (share < 0 || share > 1) throw new IllegalArgumentException("Attacker share must be between 0 and 1");
            if (delay < 0) throw new IllegalArgumentException("Block delay must be nonnegative");
            double[] runWeights = weights.clone();
            if (p.containsKey("sweep.attackerShare")) {
                if (weights.length < 2) throw new IllegalArgumentException("Attacker-share sweeps require at least two nodes");
                double others = 0;
                for (int i = 1; i < weights.length; i++) others += weights[i];
                if (others == 0 && share < 1) throw new IllegalArgumentException("Other nodes must have positive total weight for this share");
                runWeights[0] = share;
                for (int i = 1; i < weights.length; i++) runWeights[i] = others == 0 ? 0 : (1 - share) * weights[i] / others;
            }
            String[] runStrategies = base.getNodeStrategies();
            runStrategies[0] = strategy;
            for (long seed : seeds) {
                SimulationConfig config = base.toBuilder().seed(seed).hashrates(runWeights)
                    .nodeStrategies(runStrategies).blockDelay(delay).build();
                // Also resolves configured strategies/consensus before creating any files.
                new Simulation(config);
                runs.add(new Run(runs.size() + 1, config, share, strategy, delay));
            }
        }
        return new ExperimentPlan(runs, threads);
    }

    public static void validateThreads(int threads) {
        if (threads < 1 || threads > 256) throw new IllegalArgumentException("Parallel workers must be between 1 and 256");
    }
    private static String[] split(String value) {
        String[] values = value.split(",", -1);
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
            if (values[i].isEmpty()) throw new IllegalArgumentException("Empty value in batch list");
        }
        return values;
    }
    private static double[] doubles(String value, String label) {
        String[] values = split(value);
        double[] result = new double[values.length];
        Set<Double> seen = new HashSet<>();
        for (int i = 0; i < values.length; i++) {
            result[i] = Double.parseDouble(values[i]);
            if (!Double.isFinite(result[i])) throw new IllegalArgumentException(label + " must be finite");
            if (result[i] == 0) result[i] = 0; // Canonicalize negative zero.
            if (!seen.add(result[i])) throw new IllegalArgumentException("Duplicate " + label + ": " + result[i]);
        }
        return result;
    }
    public List<Run> getRuns() { return runs; }
    public int getThreads() { return threads; }
    public String getFingerprint() { return fingerprint; }

    public static final class Run {
        private final String id, fingerprint, condition, strategy;
        private final double attackerShare, blockDelay;
        private final SimulationConfig config;
        private Run(int number, SimulationConfig config, double share, String strategy, double delay) {
            this.config = config; attackerShare = share; this.strategy = strategy; blockDelay = delay;
            Properties properties = config.toProperties();
            fingerprint = ExperimentFiles.digest(ExperimentFiles.serialize(properties));
            properties.remove("seed");
            condition = ExperimentFiles.digest(ExperimentFiles.serialize(properties)).substring(0, 16);
            id = String.format(java.util.Locale.ROOT, "run-%05d-%s", number, fingerprint.substring(0, 12));
        }
        public String getId() { return id; }
        public String getFingerprint() { return fingerprint; }
        public String getCondition() { return condition; }
        public SimulationConfig getConfig() { return config; }
        public double getAttackerShare() { return attackerShare; }
        public double getBlockDelay() { return blockDelay; }
        public String getStrategy() { return strategy; }
    }
}
