# Running and extending BCASim

## Build and run

Validated with JDK 17 and Maven 3; Java 8 source/target compatibility is retained.

```sh
mvn clean verify
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/selfish-mining.properties --output runs/selfish-1
```

`--seed N` and `--duration SECONDS` override the configuration. Run `--help` for usage. The original `java -cp target/bcasim-0.0.1-SNAPSHOT.jar jp.kota.bcasim.main.Main` entry point remains usable.

With no arguments, the defaults retain the original two-node selfish/honest PoW experiment (weights 0.48/0.52, duration 100000), with seed 1. Existing output is preserved: the CLI chooses `output-file`, then `output-file-1`, etc. An explicitly supplied nonempty output directory is rejected.

## Configuration

Use a UTF-8 `.properties` file instead of editing Java constants. Unknown keys, invalid numerics, self connections and inconsistent node/matrix counts fail before the simulation starts.

| Key | Meaning |
| --- | --- |
| `seed` | Experiment seed; mining and identity random streams are independent |
| `simulation.time` | Inclusive event horizon |
| `consensus` | `PoW` or `PoS` |
| `block.interval` | Base block generation interval |
| `block.size`, `block.reward` | Block capacity and recorded reward |
| `transaction.size` | Transaction capacity |
| `nodes.weights` | Comma-separated mining/stake weights; zero-weight nodes do not mine |
| `nodes.strategies` | Per-node `honest`, `selfish`, or `double-spend` |
| `network.matrix` | Directed adjacency matrix; comma-separated columns, semicolon-separated rows |
| `network.blockDelay`, `network.transactionDelay` | Propagation delays |
| `transaction.generate` | Retained metadata; built-in strategies do not include an automatic transaction generator |

The number of nodes is derived from weights. Strategies and matrix dimensions must match. Weights are not automatically normalized; use a total of 1 for the usual interpretation. At least one node must have positive weight.

Examples: [honest](../../examples/honest.properties), [selfish mining](../../examples/selfish-mining.properties), [double spending](../../examples/double-spending.properties), [simplified PoS](../../examples/proof-of-stake.properties). PoS retains the existing deterministic interval/weight model; it does not model protocol voting, validation or finality.

## Results and replay

- `event.json`: executed events, excluding events after the horizon.
- `block.json`: first accepted blocks, including Genesis.
- `0_blockchain.json` / `.csv`: node 0's chain.
- `adjacencyMatrix.csv`: final network topology.
- `configuration.properties`: initial configuration, directly reusable with `--config`.
- `configuration.txt`: readable settings.
- `metrics.json`: event counts, final chain heights and final node metadata.
- `Mainchain.txt`: block counts per miner on node 0's selected chain.
- `attackLog.txt`: strategy messages.

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config runs/selfish-1/configuration.properties --output runs/selfish-replay
```

The same configuration, seed and built-in implementation version reproduce the same output. Custom policies, consensus, fork choice and dynamic network operations also require the same custom code/operations; they are not serialized into the properties file.

Place `adjacencyMatrix.csv`, `block.json` and `event.json` in the visualization's `output-file` directory and serve it over HTTP. The canonical visualization source is `bcasim-visualization`; its README explains deterministic synchronization to the website demo.

## Java API

```java
SimulationConfig config = SimulationConfig.builder()
    .seed(42).simulationTime(1000)
    .nodeStrategies("honest", "honest").build();
SimulationResult result = new Simulation(config).run(); // no file output
```

A Simulation is single-use. Create one per experiment, including concurrent runs. Configuration is immutable with defensive array copies. Clocks, queues, networks, chains, random streams and outputs have no mutable process-global state.

`Simulation(config, writer, consensusFactory, behaviorFactory, forkChoice)` accepts these extension points:

- `NodeBehavior` / `NodeBehaviorFactory`: initialize, receive and mine decisions; create a new behavior for each node.
- `Consensus` / `ConsensusFactory`: generation; implement `getWeight()` and both `generateBlock()` overloads.
- `ForkChoice`: selects the preferred tip. Default: longest chain, prefer own block at equal height.
- `ResultWriter`: output observer (`NOOP` by default); use `FileResultWriter(Path)` for files.

Do not share mutable custom implementations between experiments. Node owns transport, pending mining and ledger operations, while behaviors choose actions. `Simulation.createTransaction()` generates reproducible identities; `new Transaction(..., hash)` preserves a supplied identity. Transaction arrival rates, validation and handlers belong to custom behavior. The pool returns a capacity-limited snapshot without consuming transactions or validating balances.

## Migration and semantics

Replace static Scheduler/Network calls with `simulation.getScheduler()` / `simulation.getNetwork()`. The convenience classes DefaultNode, Attacker3 and Attacker4 now take Simulation as their first constructor argument. Deprecated Configuration constants remain for reference but are not runtime inputs. ResultWriter replaces OutputResult. Migrate CSV weights to `nodes.weights` and CSV topology to `network.matrix`.

Events at equal timestamps retain registration order. A newly scheduled FoundBlock replaces the pending one for the same node. Event instances are single-use and their IDs freeze upon registration. Construct a new event to reschedule.

Historical end-of-run private-block publication is retained at the last processed time. Receipt events produced by this final publication are not executed, so node tips can differ at shutdown. Cloned blocks retain shared propagation/transaction/balance state while parent/child links and receipt times remain chain-local.

Explicit fixes accompanying the structural changes: omit unexecuted beyond-horizon events from logs; honor configured PoS; fix transaction pool capacity and supplied transaction hashes; fix cache height lookup; escape JSON and serialize nonempty arrays correctly. Library errors throw exceptions instead of terminating the JVM; only the CLI exits with a nonzero failure code.

## Validation

`mvn clean verify` compares three traces recorded from the pre-refactor implementation after seed injection, including IDs, timestamps, order and block hashes. Further tests cover queue differential behavior, repeat/concurrent runs, configuration, horizon boundaries, output compatibility, replay and CLI failures. Trace provenance is in `src/test/resources/legacy-traces.md`.
