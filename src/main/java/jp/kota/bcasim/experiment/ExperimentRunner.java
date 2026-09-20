package jp.kota.bcasim.experiment;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.SimulationResult;
import jp.kota.bcasim.tool.fileio.FileResultWriter;

/** Bounded parallel execution with checked completion records and nondestructive recovery. */
public final class ExperimentRunner {
    private ExperimentRunner() {}

    /** Returns the number of failed runs. All plan validation occurs before output creation. */
    public static int run(ExperimentPlan plan, Path output, boolean resume, Integer parallel,
                          PrintStream out, PrintStream err) throws IOException {
        int workers = parallel == null ? plan.getThreads() : parallel;
        ExperimentPlan.validateThreads(workers);
        Path root = output.toAbsolutePath().normalize();
        checkDirectory(root);
        Path manifest = root.resolve("batch.properties");
        if (resume) {
            if (!Files.isRegularFile(manifest, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Cannot resume: batch.properties is missing in " + root);
            Properties previous = ExperimentFiles.read(manifest);
            if (!"1".equals(previous.getProperty("format.version")))
                throw new IOException("Cannot resume: unsupported batch manifest format");
            if (!manifest(plan).equals(previous))
                throw new IOException("Cannot resume: experiment plan differs from the saved batch; use a new output directory");
        } else if (Files.exists(root)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
                if (entries.iterator().hasNext()) throw new IOException("Batch output directory is not empty: " + root);
            }
        }
        Files.createDirectories(root);
        Path lockPath = root.resolve(".batch.lock");
        if (Files.isSymbolicLink(lockPath)) throw new IOException("Refusing symbolic link: " + lockPath);
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            FileLock lock;
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException e) { throw new IOException("This batch is already running: " + root, e); }
            if (lock == null) throw new IOException("This batch is already running: " + root);
            try (FileLock ignored = lock) {
                if (!resume) {
                    if (Files.exists(manifest, LinkOption.NOFOLLOW_LINKS))
                        throw new IOException("Another process has already initialized this batch: " + root);
                    ExperimentFiles.write(manifest, ExperimentFiles.serialize(manifest(plan)));
                }
                Path runsDirectory = root.resolve("runs");
                checkDirectory(runsDirectory);
                Files.createDirectories(runsDirectory);
                Map<String, Outcome> outcomes = new LinkedHashMap<>();
                List<ExperimentPlan.Run> pending = new ArrayList<>();
                for (ExperimentPlan.Run run : plan.getRuns()) {
                    Path directory = runsDirectory.resolve(run.getId());
                    checkDirectory(directory);
                    Properties saved = resume ? completed(run, directory) : null;
                    if (saved != null) {
                        outcomes.put(run.getId(), Outcome.success(run, saved));
                        out.println("[" + outcomes.size() + "/" + plan.getRuns().size() + "] resumed " + run.getId());
                    } else {
                        if (Files.exists(directory)) archiveIncomplete(root, run, directory);
                        pending.add(run);
                    }
                }
                execute(pending, outcomes, runsDirectory, workers, plan.getRuns().size(), out, err);
                List<Outcome> ordered = new ArrayList<>();
                for (ExperimentPlan.Run run : plan.getRuns()) ordered.add(outcomes.get(run.getId()));
                ExperimentReport.write(root, ordered, plan.getFingerprint());
                int failures = 0;
                for (Outcome result : ordered) if (result.failure != null) failures++;
                out.println("Batch: " + (ordered.size() - failures) + " completed, " + failures + " failed");
                out.println("Report: " + root.resolve("report.html"));
                return failures;
            }
        }
    }

    private static Properties manifest(ExperimentPlan plan) {
        Properties metadata = new Properties();
        metadata.setProperty("format.version", "1");
        metadata.setProperty("plan.fingerprint", plan.getFingerprint());
        metadata.setProperty("run.count", Integer.toString(plan.getRuns().size()));
        for (ExperimentPlan.Run run : plan.getRuns()) metadata.setProperty(run.getId(), run.getFingerprint());
        return metadata;
    }

    private static void execute(List<ExperimentPlan.Run> pending, Map<String, Outcome> outcomes,
                                Path directory, int workers, int total, PrintStream out, PrintStream err) throws IOException {
        if (pending.isEmpty()) return;
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(workers, pending.size()));
        CompletionService<Outcome> completion = new ExecutorCompletionService<>(executor);
        int submitted = 0;
        try {
            while (submitted < Math.min(workers, pending.size())) {
                ExperimentPlan.Run run = pending.get(submitted++);
                completion.submit(() -> executeOne(run, directory.resolve(run.getId())));
            }
            for (int finished = 0; finished < pending.size(); finished++) {
                Outcome result = completion.take().get();
                outcomes.put(result.run.getId(), result);
                out.println("[" + outcomes.size() + "/" + total + "] "
                    + (result.failure == null ? "completed " : "FAILED ") + result.run.getId());
                if (result.failure != null) err.println(result.run.getId() + ": " + result.failure);
                if (submitted < pending.size()) {
                    ExperimentPlan.Run run = pending.get(submitted++);
                    completion.submit(() -> executeOne(run, directory.resolve(run.getId())));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Batch interrupted; completed runs can be resumed", e);
        } catch (ExecutionException e) {
            throw new IOException("Batch worker failed: " + e.getCause(), e.getCause());
        } finally {
            executor.shutdownNow();
            // Keep the directory lock until workers have stopped writing, even after interruption.
            boolean interrupted = Thread.interrupted();
            for (;;) {
                try { if (executor.awaitTermination(1, TimeUnit.SECONDS)) break; }
                catch (InterruptedException e) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static Outcome executeOne(ExperimentPlan.Run run, Path directory) {
        try {
            SimulationResult result = new Simulation(run.getConfig(), new FileResultWriter(directory)).run();
            Properties values = new Properties();
            values.setProperty("run.id", run.getId());
            values.setProperty("run.fingerprint", run.getFingerprint());
            values.setProperty("seed", Long.toString(result.getSeed()));
            values.setProperty("processedEvents", Long.toString(result.getProcessedEvents()));
            values.setProperty("finalTime", Double.toString(result.getFinalTime()));
            for (Map.Entry<String, Object> metric : result.getMetrics().entrySet()) {
                Object value = metric.getValue();
                if (value == null || value instanceof Number)
                    values.setProperty("metric." + metric.getKey(), value == null ? "" : value.toString());
            }
            ExperimentFiles.write(directory.resolve("run-summary.properties"), ExperimentFiles.serialize(values));
            // Written last, after streams close. Hash every output, including replay files and metrics.
            ExperimentFiles.write(directory.resolve("completion.properties"), ExperimentFiles.serialize(checksums(run, directory)));
            return Outcome.success(run, values);
        } catch (IOException | RuntimeException e) {
            return Outcome.failure(run, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Properties completed(ExperimentPlan.Run run, Path directory) throws IOException {
        Path marker = directory.resolve("completion.properties");
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) return null;
        try {
            if (!ExperimentFiles.read(marker).equals(checksums(run, directory))) return null;
            Properties values = ExperimentFiles.read(directory.resolve("run-summary.properties"));
            if (!run.getFingerprint().equals(values.getProperty("run.fingerprint"))) return null;
            if (!run.getId().equals(values.getProperty("run.id"))) return null;
            Long.parseLong(values.getProperty("processedEvents"));
            Double.parseDouble(values.getProperty("finalTime"));
            return values;
        } catch (IOException | RuntimeException e) { return null; }
    }

    private static Properties checksums(ExperimentPlan.Run run, Path directory) throws IOException {
        Properties hashes = new Properties();
        hashes.setProperty("run.fingerprint", run.getFingerprint());
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (file.getFileName().toString().equals("completion.properties")) continue;
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unexpected result entry: " + file);
                hashes.setProperty("file." + file.getFileName(), ExperimentFiles.digest(file));
            }
        }
        if (!hashes.containsKey("file.run-summary.properties") || !hashes.containsKey("file.event.json")
            || !hashes.containsKey("file.metrics.json") || !hashes.containsKey("file.configuration.properties"))
            throw new IOException("Incomplete simulation output: " + directory);
        return hashes;
    }

    private static void archiveIncomplete(Path root, ExperimentPlan.Run run, Path directory) throws IOException {
        Path archive = root.resolve("incomplete");
        checkDirectory(archive);
        Files.createDirectories(archive);
        int number = 1;
        Path target;
        do { target = archive.resolve(run.getId() + "-" + number++); } while (Files.exists(target, LinkOption.NOFOLLOW_LINKS));
        Files.move(directory, target);
    }

    private static void checkDirectory(Path directory) throws IOException {
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Expected a directory, not a file or symbolic link: " + directory);
    }

    static final class Outcome {
        final ExperimentPlan.Run run;
        final Properties values;
        final String failure;
        private Outcome(ExperimentPlan.Run run, Properties values, String failure) {
            this.run = run; this.values = values; this.failure = failure;
        }
        static Outcome success(ExperimentPlan.Run run, Properties values) { return new Outcome(run, values, null); }
        static Outcome failure(ExperimentPlan.Run run, String failure) { return new Outcome(run, new Properties(), failure); }
    }
}
