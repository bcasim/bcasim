package jp.kota.bcasim.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.tool.fileio.FileResultWriter;

/** Regression traces captured from the instrumented pre-refactor simulator. */
public class LegacyTraceTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void honestNodesPreserveTheLegacyTrace() throws IOException {
        assertLegacyTrace("honest");
    }

    @Test
    public void selfishMiningPreservesTheLegacyTrace() throws IOException {
        assertLegacyTrace("selfish");
    }

    @Test
    public void doubleSpendPreservesTheLegacyTrace() throws IOException {
        assertLegacyTrace("double-spend");
    }

    private void assertLegacyTrace(String strategy) throws IOException {
        SimulationConfig config = SimulationConfig.builder()
                .simulationTime(1000)
                .seed(1)
                .hashrates(0.48, 0.52)
                .nodeStrategies(strategy, "honest")
                .blockInterval(10)
                .blockDelay(0)
                .adjacencyMatrix(new int[][] {{0, 1}, {1, 0}})
                .build();
        Path output = temporary.newFolder(strategy).toPath();
        new Simulation(config, new FileResultWriter(output)).run();
        String actual = new String(Files.readAllBytes(output.resolve("event.json")), StandardCharsets.UTF_8);
        String expected = readFixture("/legacy-" + strategy + ".json");
        assertEquals("All recorded fields and event order must match the legacy trace for " + strategy,
                compactJson(expected), compactJson(actual));
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(name)) {
            assertNotNull("Missing trace resource " + name, input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Ignore only formatting whitespace, preserving every character in strings. */
    private static String compactJson(String json) {
        StringBuilder compact = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) {
                compact.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') {
                quoted = true;
                compact.append(c);
            } else if (!Character.isWhitespace(c)) {
                compact.append(c);
            }
        }
        return compact.toString();
    }
}
