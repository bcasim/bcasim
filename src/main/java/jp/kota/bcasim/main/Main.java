package jp.kota.bcasim.main;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.experiment.ExperimentPlan;
import jp.kota.bcasim.experiment.ExperimentRunner;
import jp.kota.bcasim.tool.fileio.FileResultWriter;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        int status = execute(args, System.out, System.err);
        if (status != 0) System.exit(status);
    }
    public static int execute(String[] args, PrintStream out, PrintStream err) {
        try {
            Path configPath = null, outputPath = null, batchPath = null;
            Long seed = null;
            Double duration = null;
            Integer parallel = null;
            boolean resume = false;
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if ("--help".equals(option) || "-h".equals(option)) {
                    out.println("Usage: java -jar target/bcasim-0.0.1-SNAPSHOT.jar [--config FILE] [--seed N] [--duration SECONDS] [--output DIR]");
                    out.println("Batch: java -jar target/bcasim-0.0.1-SNAPSHOT.jar --batch FILE --output DIR [--resume] [--parallel N]");
                    return 0;
                }
                if (option.equals("--resume")) { resume = true; continue; }
                if (!option.equals("--config") && !option.equals("--output") && !option.equals("--seed") && !option.equals("--duration")
                    && !option.equals("--batch") && !option.equals("--parallel"))
                    throw new IllegalArgumentException("Unknown argument: " + option);
                if (++i == args.length) throw new IllegalArgumentException("Missing value for " + option);
                switch (option) {
                    case "--config": configPath = Paths.get(args[i]); break;
                    case "--output": outputPath = Paths.get(args[i]); break;
                    case "--seed": seed = Long.parseLong(args[i]); break;
                    case "--duration": duration = Double.parseDouble(args[i]); break;
                    case "--batch": batchPath = Paths.get(args[i]); break;
                    case "--parallel": parallel = Integer.parseInt(args[i]); break;
                    default: throw new AssertionError(option);
                }
            }
            if (batchPath != null) {
                if (configPath != null || seed != null || duration != null)
                    throw new IllegalArgumentException("Use base.config and seeds in the batch file; --batch cannot be combined with --config, --seed or --duration");
                if (outputPath == null) throw new IllegalArgumentException("--batch requires --output DIR");
                ExperimentPlan plan = ExperimentPlan.load(batchPath);
                return ExperimentRunner.run(plan, outputPath, resume, parallel, out, err) == 0 ? 0 : 1;
            }
            if (resume || parallel != null) throw new IllegalArgumentException("--resume and --parallel require --batch");
            SimulationConfig.Builder builder = (configPath == null ? SimulationConfig.defaults() : SimulationConfig.load(configPath)).toBuilder();
            if (seed != null) builder.seed(seed);
            if (duration != null) builder.simulationTime(duration);
            SimulationConfig config = builder.build();
            // Construct and validate strategies before creating any output directory.
            if (outputPath == null) outputPath = nextOutputDirectory();
            SimulationResult result = new Simulation(config, new FileResultWriter(outputPath)).run();
            out.println("seed=" + result.getSeed() + " events=" + result.getProcessedEvents() + " time=" + result.getFinalTime());
            out.println("Output: " + outputPath.toAbsolutePath());
            return 0;
        } catch (IOException | RuntimeException e) {
            err.println("BCASim: " + e.getMessage());
            return 1;
        }
    }
    private static Path nextOutputDirectory() {
        Path candidate = Paths.get("output-file");
        int suffix = 1;
        while (Files.exists(candidate)) candidate = Paths.get("output-file-" + suffix++);
        return candidate;
    }
}
