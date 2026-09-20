package jp.kota.bcasim.main;

import java.util.Objects;
import java.util.Random;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.node.behavior.NodeBehaviorFactory;
import jp.kota.bcasim.main.node.consensus.ConsensusFactory;
import jp.kota.bcasim.main.node.consensus.ForkChoice;
import jp.kota.bcasim.main.node.consensus.LongestChain;
import jp.kota.bcasim.network.Network;
import jp.kota.bcasim.network.NetworkGenerator;
import jp.kota.bcasim.tool.fileio.ResultWriter;

/** All mutable state and random streams belong to one experiment. */
public final class Simulation {
    private final SimulationConfig config;
    private final Random miningRandom, identityRandom;
    private final ResultWriter writer;
    private final ConsensusFactory consensusFactory;
    private final ForkChoice forkChoice;
    private final Scheduler scheduler;
    private final Network network;
    private final Blockchain blockchain;
    private boolean started;

    public Simulation(SimulationConfig config) { this(config, ResultWriter.NOOP); }
    public Simulation(SimulationConfig config, ResultWriter writer) {
        this(config, writer, ConsensusFactory.configured(), NodeBehaviorFactory.builtIn(), new LongestChain());
    }
    public Simulation(SimulationConfig config, ResultWriter writer, ConsensusFactory consensusFactory,
                      NodeBehaviorFactory behaviors, ForkChoice forkChoice) {
        this.config = Objects.requireNonNull(config, "config");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.consensusFactory = Objects.requireNonNull(consensusFactory, "consensusFactory");
        this.forkChoice = Objects.requireNonNull(forkChoice, "forkChoice");
        miningRandom = new Random(config.getSeed());
        identityRandom = new Random(config.getSeed() ^ 0x5DEECE66DL);
        scheduler = new Scheduler(this);
        network = new Network(config);
        blockchain = new Blockchain(this, "main", null);
        NetworkGenerator.populate(this, Objects.requireNonNull(behaviors, "behaviors"));
    }
    /** Single-use: construct a fresh instance for each experiment. */
    public synchronized SimulationResult run() {
        if (started) throw new IllegalStateException("A simulation can only run once");
        started = true;
        try (ResultWriter output = writer) {
            output.start(config, network);
            scheduler.InitEventList();
            scheduler.processEvent();
            output.finish(network);
            return new SimulationResult(config.getSeed(), scheduler.getSimulationTime(), scheduler.getProcessedEventCount(), network);
        }
    }
    public Transaction createTransaction(String from, String to, int value) {
        return new Transaction(from, to, value, jp.kota.bcasim.tool.HashGenerator.generateHash(
            from + ":" + to + ":" + value + ":" + identityRandom.nextLong()));
    }
    public SimulationConfig getConfig() { return config; }
    public Random getMiningRandom() { return miningRandom; }
    public Random getIdentityRandom() { return identityRandom; }
    public ResultWriter getWriter() { return writer; }
    public ConsensusFactory getConsensusFactory() { return consensusFactory; }
    public ForkChoice getForkChoice() { return forkChoice; }
    public Scheduler getScheduler() { return scheduler; }
    public Network getNetwork() { return network; }
    public Blockchain getBlockchain() { return blockchain; }
}
