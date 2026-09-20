package jp.kota.bcasim.tool.fileio;

import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.network.Network;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A single simulation's buffered outputs. Existing results are never overwritten. */
public final class FileResultWriter implements ResultWriter {
    private final Path directory;
    private final Map<String, Long> eventCounts = new LinkedHashMap<>();
    private SimulationConfig config;
    private JsonArrayWriter events;
    private JsonArrayWriter blocks;
    private BufferedWriter attacks;
    private long blockCount;
    private boolean started;
    private boolean finished;
    private boolean closed;

    public FileResultWriter(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public void start(SimulationConfig config, Network network) {
        if (started || closed) throw new IllegalStateException("Result writer can only be started once");
        this.config = Objects.requireNonNull(config, "config");
        Objects.requireNonNull(network, "network");
        try {
            Files.createDirectories(directory);
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
                if (entries.iterator().hasNext()) {
                    throw new IOException("Output directory is not empty: " + directory);
                }
            }
            events = new JsonArrayWriter(open(directory.resolve("event.json")));
            blocks = new JsonArrayWriter(open(directory.resolve("block.json")));
            attacks = open(directory.resolve("attackLog.txt"));
            started = true;
            recordBlock(network.getNodeList().get(0).getBlockchain().getGenesis());
        } catch (IOException failure) {
            try { close(); } catch (UncheckedIOException closeFailure) { failure.addSuppressed(closeFailure); }
            throw new UncheckedIOException("Cannot start result output in " + directory + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public void recordEvent(Event event) {
        requireRecording();
        eventCounts.merge(event.getEventType(), 1L, Long::sum);
        // Local receipt is already represented by FoundBlock in the visualizer.
        if (event instanceof ReceiveBlock && ((ReceiveBlock) event).getFrom() == event.getNode()) return;
        try {
            events.append(ResultRecords.event(event));
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot record event", failure);
        }
    }

    @Override
    public void recordBlock(Block block) {
        requireRecording();
        try {
            blocks.append(ResultRecords.block(block, config.getBlockReward()));
            blockCount++;
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot record block", failure);
        }
    }

    @Override
    public void recordAttack(String message) {
        requireRecording();
        try {
            attacks.write(message);
            attacks.newLine();
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot record attack", failure);
        }
    }

    @Override
    public void finish(Network network) {
        requireRecording();
        try {
            ResultFiles.write(directory, config, network, eventCounts, blockCount);
            finished = true;
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot finish result output", failure);
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        IOException failure = null;
        for (Closeable resource : new Closeable[] { events, blocks, attacks }) {
            if (resource == null) continue;
            try {
                resource.close();
            } catch (IOException closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
        }
        if (failure != null) throw new UncheckedIOException("Cannot close result output", failure);
    }

    static BufferedWriter open(Path file) throws IOException {
        return Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    private void requireRecording() {
        if (!started || finished || closed) throw new IllegalStateException("Result writer is not recording");
    }
}
