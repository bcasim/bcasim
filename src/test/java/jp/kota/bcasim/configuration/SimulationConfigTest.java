package jp.kota.bcasim.configuration;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SimulationConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void arraysAreCopiedOnInputAndOutput() {
        double[] rates = {0.3, 0.7}; int[][] matrix = {{0,1},{1,0}}; String[] names = {"honest", "honest"};
        SimulationConfig config = SimulationConfig.builder().hashrates(rates).adjacencyMatrix(matrix).nodeStrategies(names).build();
        rates[0] = 9; matrix[0][1] = 0; names[0] = "unknown";
        config.getHashrates()[0] = 8; config.getAdjacencyMatrix()[0][1] = 0; config.getNodeStrategies()[0] = "other";
        assertEquals(0.3, config.getHashrates()[0], 0);
        assertEquals(1, config.getAdjacencyMatrix()[0][1]);
        assertEquals("honest", config.getNodeStrategies()[0]);
    }
    @Test public void rejectsInvalidNumericsAndInconsistentTopology() {
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().simulationTime(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().blockInterval(0).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().hashrates(0,0).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().hashrates(-1,1).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().blockDelay(-1).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().hashrates(1).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().adjacencyMatrix(new int[][]{{1,0},{0,0}}).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().adjacencyMatrix(new int[][]{{0,2},{1,0}}).build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().nodeStrategies("honest").build());
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.builder().consensus("typo").build());
    }
    @Test public void loadsPropertiesAndRejectsTyposRatherThanUsingDefaults() throws Exception {
        Path file = temporary.newFile("run.properties").toPath();
        Files.write(file, ("seed=99\nsimulation.time=10\nnodes.weights=1\nnodes.strategies=honest\nnetwork.matrix=0\nconsensus=PoS\n").getBytes(StandardCharsets.UTF_8));
        SimulationConfig config = SimulationConfig.load(file);
        assertEquals(99, config.getSeed()); assertEquals(1, config.getNumberOfNodes()); assertEquals("PoS", config.getConsensus());
        Files.write(file, "simulation.tiem=10".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.load(file));
        Files.write(file, "transaction.generate=yes".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.load(file));
    }
}
