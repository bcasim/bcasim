# Batch experiments and comparison reports

Build with `mvn test package`, then run the supplied 60-run comparison:

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar \
  --batch examples/batch-comparison.properties --output experiments/comparison --parallel 2
```

Open `experiments/comparison/report.html` in a browser. It works offline and includes SVG charts, means, pointwise 95% confidence intervals, per-metric sample counts, and links to per-run configurations. `summary.csv` contains one row per run; `aggregates.csv` contains one row per condition and metric. Both files use quoted UTF-8 CSV with a header.

## Configure the grid

```properties
base.config=selfish-mining.properties
seeds=1,2,3,4,5
sweep.attackerShare=0.2,0.35,0.48
sweep.blockDelay=0,2
sweep.strategy=honest,selfish
threads=2
```

`base.config` is required and resolved relative to the batch file. It accepts the same simulation configuration used by `--config`, including transaction settings, the observer node, and timed network changes. Every combination of the three sweep dimensions runs once for each seed. In this example: 3 shares × 2 delays × 2 strategies × 5 seeds = 60 simulations. All input combinations and strategies are validated before any output directory is created.

| Batch key | Meaning | Default |
|---|---|---|
| `base.config` | Simulation `.properties` file | Required |
| `seeds` | Comma-separated unique integer seeds | Base configuration seed |
| `sweep.attackerShare` | Node 0 weight as a share in [0, 1] | Keep base weights unchanged |
| `sweep.blockDelay` | Comma-separated nonnegative block delays, in seconds | Base block delay |
| `sweep.strategy` | Node 0 strategy: `honest`, `selfish`, `double-spend` | Base node 0 strategy |
| `threads` | Concurrent simulations, 1–256 | 1 |

When a share sweep is supplied, total node weight becomes 1. Node 0 receives the selected share; the remaining share is distributed among other nodes proportionally to their base weights. Other nodes' strategies remain unchanged. This requires at least two nodes, and positive total weight among the other nodes when the selected share is less than 1. If no share sweep is supplied, the original weights, including their total, are preserved. `honest` is a useful baseline even though the parameter and metric names use “attacker.”

Seeds and sweep values must be unique; empty lists, nonfinite numbers, unknown keys, unsupported strategies, and plans above 10,000 runs are rejected. `--parallel N` overrides `threads` and may be changed when resuming. `--batch` requires an explicit output directory and cannot be combined with `--config`, `--seed`, or `--duration`.

## Resume after interruption

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar \
  --batch examples/batch-comparison.properties --output experiments/comparison --resume --parallel 4
```

The output directory must otherwise be empty for a new batch. A saved manifest records the full expanded configuration fingerprint, deterministic run identifiers, and format version. Resume requires an identical experiment plan and compatible manifest format; changing seeds, conditions, or any effective base setting requires a new output directory. Keep the same simulator build when resuming; manifests identify inputs and persistence format, not the simulator source revision.

Each simulation writes its usual replay files, including `event.json`, `block.json`, `metrics.json`, and `configuration.properties`, into `runs/run-NNNNN-HASH/`. A `run-summary.properties` file saves the numeric summary for offline reconstruction. `completion.properties` is written last, after output streams close, and contains SHA-256 hashes of all run files. Resume skips only runs with a valid matching completion record. Missing, incomplete, or damaged run directories are preserved under `incomplete/` and rerun. Completed runs are not modified; the top-level report and CSVs are rebuilt in deterministic plan order. A directory lock prevents concurrent batch writers.

Progress is printed as runs complete. Run failures appear in the summary/report and produce a nonzero command exit status. Successful runs remain resumable. Parallel scheduling does not change seeds, per-run outputs, or report ordering. Parallelism increases memory and disk demand because each simulation has its own state and replay log.

## Interpret the metrics

Chain metrics refer to the configured observer's final chain and exclude genesis. The report compares node 0's accepted block share, stale fraction, fork/reorganization statistics, propagation delay, structural attack outcomes, and confirmed transactions. See the [main guide](english/doc.md) for definitions and modeling limits. In particular, the attack state machine's success rate does not establish that a conflicting payment was reversed.

For each condition, the report computes the arithmetic mean across seeds, sample standard deviation, and pointwise Student-t 95% confidence interval for the mean. Its sample count is the number of successful runs with a defined value for that metric. Rates are averaged with equal weight per run; they are not pooled by summing numerators and denominators. Undefined rates or propagation delays remain blank in CSVs and are excluded, not replaced with zero. A single observation has no interval. Intervals for ratios are not clipped to [0, 1].

The same seed list is reused for each condition, but these intervals describe each condition separately; they are not paired difference tests, multiple-comparison adjustments, or certainty bounds on an individual run. Sparse outcomes and small samples can make the approximation unreliable. Use more seeds and a sufficient simulation duration before drawing conclusions.
