package jp.kota.bcasim.experiment;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.TreeSet;

/** Stable, timestamp-free persistence for experiment plans and completion records. */
final class ExperimentFiles {
    private ExperimentFiles() {}
    static Properties read(Path path) throws IOException {
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
        return values;
    }
    static String serialize(Properties values) {
        StringBuilder result = new StringBuilder();
        for (String key : new TreeSet<>(values.stringPropertyNames()))
            result.append(escape(key)).append('=').append(escape(values.getProperty(key))).append('\n');
        return result.toString();
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
            .replace("\t", "\\t").replace("\f", "\\f").replace(" ", "\\ ")
            .replace("=", "\\=").replace(":", "\\:").replace("#", "\\#").replace("!", "\\!");
    }
    static String digest(String value) { return digest(value.getBytes(StandardCharsets.UTF_8)); }
    static String digest(byte[] value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static String digest(Path file) throws IOException {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[65536];
                int length;
                while ((length = input.read(buffer)) != -1) hash.update(buffer, 0, length);
            }
            StringBuilder result = new StringBuilder();
            for (byte b : hash.digest()) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static void write(Path path, String value) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("Refusing symbolic link: " + path);
        Path temporary = Files.createTempFile(path.getParent(), ".batch-", ".tmp");
        try {
            Files.write(temporary, value.getBytes(StandardCharsets.UTF_8));
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
