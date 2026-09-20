package jp.kota.bcasim.experiment;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jp.kota.bcasim.main.Main;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExperimentTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final PrintStream quiet = new PrintStream(new ByteArrayOutputStream());

    private Path plan(String extra) throws Exception {
        Path directory = temporary.newFolder().toPath();
        Files.write(directory.resolve("base.properties"), ("simulation.time=60\nblock.interval=3\n"
            + "transaction.generate=false\nnodes.weights=0.3,0.7\nnodes.strategies=honest,honest\n").getBytes(StandardCharsets.UTF_8));
        Path batch = directory.resolve("batch.properties");
        Files.write(batch, ("base.config=base.properties\nseeds=11,22\n" + extra).getBytes(StandardCharsets.UTF_8));
        return batch;
    }
    private Path output(String name) { return temporary.getRoot().toPath().resolve(name); }
    private void run(ExperimentPlan plan, Path directory, boolean resume, int workers) throws Exception {
        assertEquals(0, ExperimentRunner.run(plan, directory, resume, workers, quiet, quiet));
    }

    @Test public void expandsCartesianProductAndPreservesRelativeHonestWeights() throws Exception {
        Path batch = plan("sweep.attackerShare=0.2,0.4\nsweep.blockDelay=0,2\nsweep.strategy=honest,selfish\nthreads=2\n");
        Files.write(batch.getParent().resolve("base.properties"), ("simulation.time=10\ntransaction.generate=false\n"
            + "nodes.weights=2,3,5\nnodes.strategies=honest,honest,honest\nnetwork.matrix=0,1,1;1,0,1;1,1,0\n").getBytes(StandardCharsets.UTF_8));
        ExperimentPlan plan = ExperimentPlan.load(batch);
        assertEquals(16, plan.getRuns().size());
        assertEquals(2, plan.getThreads());
        assertArrayEquals(new double[] {0.2, 0.3, 0.5}, plan.getRuns().get(0).getConfig().getHashrates(), 1e-12);
        assertEquals(plan.getRuns().get(0).getCondition(), plan.getRuns().get(1).getCondition());
        assertNotEquals(plan.getRuns().get(0).getId(), plan.getRuns().get(1).getId());
    }

    @Test public void invalidPlansWriteNothingEvenWhenInvalidValueOccursLast() throws Exception {
        Path batch = plan("sweep.strategy=honest,unknown\n");
        Path output = output("invalid");
        assertEquals(1, Main.execute(new String[] {"--batch", batch.toString(), "--output", output.toString()}, quiet, quiet));
        assertFalse(Files.exists(output));
    }

    @Test public void rejectsDuplicateSeedsInvalidSharesAndUnknownKeys() throws Exception {
        for (String extra : new String[] {"seeds=1,1\n", "sweep.attackerShare=1.01\n", "sweep.blockDelay=NaN\n", "unknown=1\n", "threads=0\n", "sweep.strategy=honest,honest\n"}) {
            Path batch = plan(extra);
            try { ExperimentPlan.load(batch); fail(extra); }
            catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
        }
    }

    @Test public void serialAndParallelRunsAndReportsAreByteIdentical() throws Exception {
        ExperimentPlan plan = ExperimentPlan.load(plan("sweep.strategy=honest,selfish\nsweep.blockDelay=1\n"));
        Path serial = output("serial"), parallel = output("parallel");
        run(plan, serial, false, 1);
        run(plan, parallel, false, 3);
        List<Path> files;
        try (Stream<Path> paths = Files.walk(serial)) { files = paths.filter(Files::isRegularFile).collect(Collectors.toList()); }
        for (Path file : files) assertArrayEquals(serial.relativize(file).toString(), Files.readAllBytes(file), Files.readAllBytes(parallel.resolve(serial.relativize(file))));
        String report = new String(Files.readAllBytes(serial.resolve("report.html")), StandardCharsets.UTF_8);
        assertTrue(report.contains("<svg"));
        assertTrue(report.contains("Student-t"));
        assertFalse(report.contains("<script"));
        assertEquals(5, Files.readAllLines(serial.resolve("summary.csv"), StandardCharsets.UTF_8).size());
    }

    @Test public void resumeSkipsCompleteRunsAndRebuildsMissingSummary() throws Exception {
        ExperimentPlan plan = ExperimentPlan.load(plan("")); Path directory = output("resume");
        run(plan, directory, false, 1);
        Path marker = directory.resolve("runs").resolve(plan.getRuns().get(0).getId()).resolve("completion.properties");
        java.nio.file.attribute.FileTime time = Files.getLastModifiedTime(marker);
        byte[] summary = Files.readAllBytes(directory.resolve("summary.csv"));
        Files.delete(directory.resolve("summary.csv"));
        run(plan, directory, true, 2);
        assertEquals(time, Files.getLastModifiedTime(marker));
        assertArrayEquals(summary, Files.readAllBytes(directory.resolve("summary.csv")));
        assertFalse(Files.exists(directory.resolve("incomplete")));
    }

    @Test public void resumeArchivesCorruptAndInterruptedRunsThenRecreatesThem() throws Exception {
        ExperimentPlan plan = ExperimentPlan.load(plan("")); Path directory = output("recovery");
        run(plan, directory, false, 2);
        Path first = directory.resolve("runs").resolve(plan.getRuns().get(0).getId());
        Path second = directory.resolve("runs").resolve(plan.getRuns().get(1).getId());
        byte[] expectedEvents = Files.readAllBytes(first.resolve("event.json"));
        Files.write(first.resolve("event.json"), "broken".getBytes(StandardCharsets.UTF_8));
        Files.delete(second.resolve("completion.properties"));
        run(plan, directory, true, 2);
        assertArrayEquals(expectedEvents, Files.readAllBytes(first.resolve("event.json")));
        assertEquals("broken", new String(Files.readAllBytes(directory.resolve("incomplete").resolve(first.getFileName() + "-1").resolve("event.json")), StandardCharsets.UTF_8));
        assertTrue(Files.exists(directory.resolve("incomplete").resolve(second.getFileName() + "-1").resolve("event.json")));
    }

    @Test public void changedPlanOrManifestVersionCannotResumeExistingBatch() throws Exception {
        Path file = plan(""); ExperimentPlan plan = ExperimentPlan.load(file); Path directory = output("mismatch");
        run(plan, directory, false, 1);
        byte[] report = Files.readAllBytes(directory.resolve("report.html"));
        Files.write(file, "base.config=base.properties\nseeds=33,44\n".getBytes(StandardCharsets.UTF_8));
        try { run(ExperimentPlan.load(file), directory, true, 1); fail("changed seeds"); }
        catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("differs")); }
        Properties manifest = ExperimentFiles.read(directory.resolve("batch.properties"));
        manifest.setProperty("format.version", "999");
        Files.write(directory.resolve("batch.properties"), ExperimentFiles.serialize(manifest).getBytes(StandardCharsets.UTF_8));
        try { run(plan, directory, true, 1); fail("future format"); }
        catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("format")); }
        assertArrayEquals(report, Files.readAllBytes(directory.resolve("report.html")));
    }

    @Test public void outputCannotBeOverwrittenWithoutResumeAndCliRejectsConflictingModes() throws Exception {
        Path file = plan(""); Path directory = output("cli");
        assertEquals(0, Main.execute(new String[] {"--batch", file.toString(), "--output", directory.toString(), "--parallel", "2"}, quiet, quiet));
        assertEquals(1, Main.execute(new String[] {"--batch", file.toString(), "--output", directory.toString()}, quiet, quiet));
        assertEquals(1, Main.execute(new String[] {"--batch", file.toString(), "--seed", "1", "--output", output("unused").toString()}, quiet, quiet));
        assertEquals(1, Main.execute(new String[] {"--parallel", "2"}, quiet, quiet));
        assertFalse(Files.exists(output("unused")));
    }

    @Test public void undefinedMetricsRemainBlankAndDoNotBecomeZeros() throws Exception {
        Path file = plan("");
        Files.write(file.getParent().resolve("base.properties"), "simulation.time=0\ntransaction.generate=false\n".getBytes(StandardCharsets.UTF_8));
        ExperimentPlan plan = ExperimentPlan.load(file); Path directory = output("empty");
        run(plan, directory, false, 1);
        String aggregate = new String(Files.readAllBytes(directory.resolve("aggregates.csv")), StandardCharsets.UTF_8);
        assertTrue(aggregate.contains("\"attackSuccessRate\",\"0\",\"\",\"\",\"\",\"\""));
    }

    @Test public void statisticsUseSampleVarianceAndStudentTIncludingLargeBatches() {
        ExperimentReport.Statistics s = new ExperimentReport.Statistics();
        s.add(1); s.add(2); s.add(3);
        assertEquals(2, s.mean, 1e-12); assertEquals(1, s.sd(), 1e-12);
        assertEquals(4.302652730 / Math.sqrt(3), s.margin(), 1e-9);
        assertEquals(1.98421695, ExperimentReport.Statistics.t975(99), 1e-6);
        assertEquals(1.960201, ExperimentReport.Statistics.t975(9999), 1e-6);
    }
}
