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
| `seed` | Experiment seed; mining, identity and transaction random streams are independent |
| `simulation.time` | Inclusive event horizon |
| `consensus` | `PoW` or `PoS` |
| `block.interval` | Base block generation interval |
| `block.size`, `block.reward` | Block capacity and recorded reward |
| `transaction.size` | Transaction capacity |
| `nodes.weights` | Comma-separated mining/stake weights; zero-weight nodes do not mine |
| `nodes.strategies` | Per-node `honest`, `selfish`, or `double-spend` |
| `network.matrix` | Directed adjacency matrix; comma-separated columns, semicolon-separated rows |
| `network.blockDelay`, `network.transactionDelay` | Propagation delays |
| `transaction.generate` | Enable automatic workload (`true` by default; also requires positive rate) |
| `transaction.rate` | Network-wide Poisson arrival attempts per simulation time unit; default `0` disables workload |
| `transaction.value` | Positive integer transfer amount per generated transaction; default `1` |
| `transaction.initialBalance` | Initial balance of every node account; default `1000` |
| `observer.node` | Node index whose selected chain is used for metrics; default `0` |
| `network.changes` | Semicolon-separated `time:connect:from:to` or `time:disconnect:from:to` entries |

The number of nodes is derived from weights. Strategies and matrix dimensions must match. Weights are not automatically normalized; use a total of 1 for the usual interpretation. At least one node must have positive weight.

Examples: [honest](../../examples/honest.properties), [selfish mining](../../examples/selfish-mining.properties), [double spending](../../examples/double-spending.properties), [simplified PoS](../../examples/proof-of-stake.properties). PoS retains the existing deterministic interval/weight model; it does not model protocol voting, validation or finality.

## Results and replay

- `event.json`: executed events, excluding events after the horizon.
- `block.json`: first accepted blocks, including Genesis.
- `0_blockchain.json` / `.csv`: node 0's chain.
- `adjacencyMatrix.csv`: final network topology.
- `initialAdjacencyMatrix.csv`: initial topology for replaying network changes.
- `configuration.properties`: initial configuration, directly reusable with `--config`.
- `configuration.txt`: readable settings.
- `metrics.json`: event counts, final node metadata, selected-chain statistics, transaction balances and pending pools; definitions below.
- `Mainchain.txt`: block counts per miner on node 0's selected chain.
- `attackLog.txt`: strategy messages.

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config runs/selfish-1/configuration.properties --output runs/selfish-replay
```

The same configuration, seed and built-in implementation version reproduce the same output. Configured `network.changes` and transaction inputs are included in replay files. Custom policies, consensus, fork choice and network operations injected from Java also require the same custom code; they are not serialized.

Open the visualization over HTTP and select or drop `adjacencyMatrix.csv`, `block.json` and `event.json` together. Include `initialAdjacencyMatrix.csv` for network changes and `metrics.json` for simulator metrics. You can seek, step through events/blocks, inspect details and compare another run's metrics. Alternatively place the files in `output-file/`. The visualizer can download a configuration file; execute it using the Java CLI. Its README explains website demo synchronization.

## Batch experiments

Use `--batch PLAN --output DIRECTORY`, with optional `--parallel N` and `--resume`. A plan combines seeds with sweeps of node 0's strategy, mining share and block delay. Results include per-run replay files, `summary.csv` and an offline `report.html` with charts and pointwise 95% confidence intervals across seeds. Resume verifies configuration fingerprints and result checksums before skipping a run. See the [batch guide](../experiments-en.md) and [example plan](../../examples/batch-comparison.properties).

## Transactions and network changes

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/transactions.properties --output runs/transactions
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/partition.properties --output runs/partition
```

Accounts are node IDs (`0`, `1`, …). A transaction transfers an integer value and consumes the sender's next nonce; conflicting spends, duplicate transactions and insufficient balances are rejected. Mining rewards are applied after the block's transactions. Generated transactions sample a distinct recipient and are skipped if the sender cannot afford them. `transaction.rate=0` keeps transaction-free legacy experiments unchanged. This is an account model without signatures, fees, UTXOs or merchant confirmation logic. The built-in `double-spend` strategy still measures private/public branch races; it does not automatically create two economic payments. See the [transaction model](../transaction-model.md).

`network.changes=200:disconnect:0:1;600:connect:0:1` changes the directed link from node 0 to node 1. Add the reverse direction to partition both ways. Times must fall within the simulation horizon. Equal-time changes follow their configuration order. Disconnecting prevents new sends; messages already in flight still arrive. Reconnecting sends missing public history to the receiving node with normal block delay; withheld private blocks remain private. Unknown-parent block arrivals wait for their ancestors. Transaction pools are not synchronized on reconnection.

## Metric definitions

Metrics are a final snapshot, excluding Genesis. `observer.node` chooses the chain used below; `0_blockchain.*` and `Mainchain.txt` remain node 0 exports for compatibility.

| Metric | Definition |
| --- | --- |
| `acceptedBlocks`, `mainchainByMiner` | Blocks on the observer's selected chain, total and per miner |
| `totalPublishedBlocks` | Unique blocks accepted into the simulation's public aggregate tree |
| `staleBlocks`, `staleFraction` | Published blocks outside the observer's selected chain, and their fraction of published blocks |
| `attackerRevenueShare` | Node 0's fraction of accepted blocks; a block share, without fees or monetary profit |
| `forkPoints` | Observer-tree parents with at least two accepted children |
| `reorgCount`, `maxReorgDepth` | Observer tip changes that detach blocks; maximum number detached in one change |
| `remoteBlockReceipts`, `meanPropagationDelay` | Unique non-miner node/block acceptances, and mean delay from the block's first public acceptance to each such acceptance |
| `attackSuccesses`, `attackFailures`, `attackSuccessRate` | Completed built-in double-spend branch-race trials summed across nodes; success / completed trials |
| `transactionsConfirmed` | Unique transactions on the observer's selected chain |
| `canonicalBalances`, `canonicalNonces` | Per-account balance and next nonce at the observer's selected tip, including initial funds and block rewards |
| `pendingTransactions`, `pendingOrphanBlocks`, `rejectedBlocks` | Per-node pool size, unknown-parent blocks awaiting ancestors, and invalid received-block rejection count |

Ratios and averages with no observations are JSON `null`, not zero. Propagation delay includes partition and missing-parent waiting time. During partitions, off-chain blocks may simply be unknown to the observer; “stale” is a snapshot measure, not permanent invalidity. Historical final private-block publication is included in published totals, but its receipt events are not processed. Compare like-for-like duration, observer and strategy settings.

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

Do not share mutable custom implementations between experiments. Node owns transport, pending mining and ledger operations, while behaviors choose actions. `Simulation.createTransaction()` generates reproducible identities; `new Transaction(..., hash)` preserves a supplied identity. The automatic workload uses its own random stream. The pool validates account balances and nonces, returns a capacity-limited snapshot, removes transactions confirmed on the selected chain, and requeues valid transactions from detached blocks. Use `createTransaction(from, to, value, nonce)` for deliberate conflicts or branch-specific nonces; the three-argument helper assigns consecutive nonces per sender.

## Migration and semantics

Replace static Scheduler/Network calls with `simulation.getScheduler()` / `simulation.getNetwork()`. The convenience classes DefaultNode, Attacker3 and Attacker4 now take Simulation as their first constructor argument. Deprecated Configuration constants remain for reference but are not runtime inputs. ResultWriter replaces OutputResult. Migrate CSV weights to `nodes.weights` and CSV topology to `network.matrix`.

Events at equal timestamps retain registration order. A newly scheduled FoundBlock replaces the pending one for the same node. Event instances are single-use and their IDs freeze upon registration. Construct a new event to reschedule.

Historical end-of-run private-block publication is retained at the last processed time. Receipt events produced by this final publication are not executed, so node tips can differ at shutdown. Cloned blocks share payload and propagation state while parent/child links and receipt times remain chain-local. Every accepted block has an immutable account ledger, recomputed against its parent; the legacy balance-list field is not the source of ledger validation.

Explicit fixes accompanying the structural changes: omit unexecuted beyond-horizon events from logs; honor configured PoS; fix transaction pool capacity and supplied transaction hashes; fix cache height lookup; escape JSON and serialize nonempty arrays correctly. Library errors throw exceptions instead of terminating the JVM; only the CLI exits with a nonzero failure code.

## Validation

`mvn clean verify` compares three traces recorded from the pre-refactor implementation after seed injection, including IDs, timestamps, order and block hashes. Further tests cover queue differential behavior, repeat/concurrent runs, configuration, horizon boundaries, output compatibility, replay and CLI failures. Trace provenance is in `src/test/resources/legacy-traces.md`.
