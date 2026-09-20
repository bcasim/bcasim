package jp.kota.bcasim.tool.fileio;

import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.network.Network;

/** Receives simulation observations without coupling the engine to a file format. */
public interface ResultWriter extends AutoCloseable {
    ResultWriter NOOP = new ResultWriter() { };

    default void start(SimulationConfig config, Network network) { }

    default void recordEvent(Event event) { }

    default void recordBlock(Block block) { }

    default void recordAttack(String message) { }

    default void finish(Network network) { }

    @Override
    default void close() { }
}
