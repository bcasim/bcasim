package jp.kota.bcasim.tool.fileio;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/** Minimal JSON encoding for output records, including control characters and Unicode. */
final class JsonWriter {
    private JsonWriter() { }

    static void write(Writer writer, Object value) throws IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof Map) {
            writer.write('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) writer.write(',');
                first = false;
                writeString(writer, String.valueOf(entry.getKey()));
                writer.write(':');
                write(writer, entry.getValue());
            }
            writer.write('}');
        } else if (value instanceof Iterable) {
            writer.write('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) writer.write(',');
                first = false;
                write(writer, item);
            }
            writer.write(']');
        } else if (value instanceof Number || value instanceof Boolean) {
            if (value instanceof Number && !Double.isFinite(((Number) value).doubleValue())) {
                throw new IllegalArgumentException("JSON numbers must be finite");
            }
            writer.write(value.toString());
        } else {
            writeString(writer, value.toString());
        }
    }

    private static void writeString(Writer writer, String value) throws IOException {
        writer.write('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': writer.write("\\\""); break;
                case '\\': writer.write("\\\\"); break;
                case '\b': writer.write("\\b"); break;
                case '\f': writer.write("\\f"); break;
                case '\n': writer.write("\\n"); break;
                case '\r': writer.write("\\r"); break;
                case '\t': writer.write("\\t"); break;
                default:
                    if (c < 0x20 || Character.isSurrogate(c) || c == '\u2028' || c == '\u2029') {
                        writer.write("\\u");
                        String hex = Integer.toHexString(c);
                        for (int j = hex.length(); j < 4; j++) writer.write('0');
                        writer.write(hex);
                    } else {
                        writer.write(c);
                    }
            }
        }
        writer.write('"');
    }
}
