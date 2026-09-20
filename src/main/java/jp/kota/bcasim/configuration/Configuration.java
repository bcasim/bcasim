package jp.kota.bcasim.configuration;

/** @deprecated Use SimulationConfig. These constants are legacy defaults, not runtime state. */
@Deprecated
public final class Configuration {
    private Configuration() {}
    public static final double SIMULATION_TIME = 100000;
    public static final double BLOCK_INTERVAL = 10;
    public static final String CONSENSUS = "PoW";
    public static final double BLOCK_SIZE = 8;
    public static final double BLOCK_REWARD = 10;
    public static final double TRANSACTION_SIZE = 1;
    public static final String GENERATE_TRANSACTION = "YES";
    public static final int NUMBER_OF_NODES = 2;
    public static final double[] HASHRATE_LIST = {0.48, 0.52};
    public static final double BLOCK_DELAY = 0;
    public static final double TRANSACTION_DELAY = 15;
    public static final int[][] ADJACENCY_MATRIX = {{0, 1}, {1, 0}};
}
