package jp.kota.bcasim.tool.fileio;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;

/** Streams a JSON array without retaining the entire simulation log in memory. */
final class JsonArrayWriter implements Closeable {
    private final BufferedWriter writer;
    private boolean first = true;
    private boolean closed;

    JsonArrayWriter(BufferedWriter writer) throws IOException {
        this.writer = writer;
        try {
            writer.write('[');
        } catch (IOException failure) {
            try { writer.close(); } catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    void append(Object record) throws IOException {
        if (closed) throw new IllegalStateException("JSON output is closed");
        if (!first) writer.write(',');
        writer.newLine();
        JsonWriter.write(writer, record);
        first = false;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            writer.newLine();
            writer.write(']');
            writer.newLine();
        } finally {
            writer.close();
        }
    }
}
