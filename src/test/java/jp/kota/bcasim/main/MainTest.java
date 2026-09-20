package jp.kota.bcasim.main;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MainTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void cliRunsWithoutChangingSourceAndRefusesToOverwriteResults() throws Exception {
        Path output = temporary.getRoot().toPath().resolve("result");
        ByteArrayOutputStream messages = new ByteArrayOutputStream(); PrintStream stream = new PrintStream(messages);
        String[] args = {"--seed", "123", "--duration", "30", "--output", output.toString()};
        assertEquals(0, Main.execute(args, stream, stream));
        byte[] original = Files.readAllBytes(output.resolve("event.json"));
        assertTrue(messages.toString("UTF-8").contains("seed=123"));
        assertEquals(1, Main.execute(args, stream, stream));
        assertArrayEquals(original, Files.readAllBytes(output.resolve("event.json")));
    }
    @Test public void helpAndBadArgumentsHaveUsefulExitCodes() {
        PrintStream stream = new PrintStream(new ByteArrayOutputStream());
        assertEquals(0, Main.execute(new String[]{"--help"}, stream, stream));
        assertEquals(1, Main.execute(new String[]{"--missing"}, stream, stream));
        assertEquals(1, Main.execute(new String[]{"--seed"}, stream, stream));
        assertEquals(1, Main.execute(new String[]{"--duration", "NaN"}, stream, stream));
    }
}
