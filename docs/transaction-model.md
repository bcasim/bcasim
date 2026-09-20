# Transaction and ledger model / トランザクション・台帳モデル

BCASim can generate account transfers and validate them on each public or private branch. This is a deterministic research model, not a cryptocurrency implementation: account IDs are node IDs; there are no signatures, private keys, fees, UTXOs, transaction replacement policies or economic double-spend attack generator. The existing `double-spend` strategy still measures a private-chain race. Its success counter does not prove a merchant payment was reversed.

BCASimでは、口座間の送金を自動生成し、公開・非公開の各チェーン上で検証できます。口座IDはノードIDと同じです。署名・秘密鍵・手数料・UTXO・取引置換ポリシーはモデル化していません。既存の`double-spend`戦略の成功数は非公開チェーン競争の構造上の成功であり、取引の二重支払いが成立したことを示しません。

## Workload configuration / 自動生成の設定

```properties
transaction.generate=true
transaction.rate=2
transaction.value=1
transaction.initialBalance=1000
network.transactionDelay=0.2
```

- `transaction.rate` is the total arrival-attempt rate across the network, in attempts per simulation second, not a per-node rate. Arrivals follow an exponential waiting-time distribution. `0` (the default) disables generation and preserves legacy empty-block traces. `transaction.generate=false` also disables generation.
- Each attempt chooses a uniformly random sender and a different recipient. A `SendTransaction` event is produced only if the sender has enough balance after accounting for valid pending transactions. The arrival attempt is still recorded as `GenerateTransaction` when funds are insufficient. At least two nodes are required when generation is enabled.
- `transaction.value` is the positive integer transfer amount. `transaction.initialBalance` is the starting balance of every account; all balances are nonnegative. Every accepted canonical block credits `block.reward` to its miner after applying the block's transfers.
- `network.transactionDelay` is the delay of each directed transmission. Block payloads are selected when mining begins; transfers arriving during mining become eligible for later blocks.
- The workload and transaction identifiers use their own seeded random stream. They do not consume the mining-time or block-identity random streams. Only one future arrival is scheduled at a time, and no arrival is scheduled beyond `simulation.time`.

`transaction.rate`はネットワーク全体の1シミュレーション秒あたりの生成試行数です。送信者・受信者をランダムに選び、保留中の有効な取引を反映しても送信者の残高が足りる場合に送信します。残高不足で送信されなかった試行は`GenerateTransaction`として残ります。マイニング開始時にブロック内容が確定するため、その後に届いた取引は次回以降の候補になります。

## Validation and reorganizations / 検証とチェーン再編

Each sender has a nonce starting at `0`. A transaction is valid on a branch only when its nonce equals that account's next nonce and the current balance covers its value. Applying it debits the sender, credits the recipient and increments the sender's nonce. A conflicting transaction with the same sender and nonce may be valid on an alternative branch, but both cannot be applied on one branch.

The pool deduplicates transaction hashes. Selection enforces the configured block capacity and applies transactions in a valid nonce order; nonce gaps and temporarily unaffordable transactions remain pending. Invalid account names, already spent nonces and individually unaffordable transfers are rejected at ingress. When the canonical tip changes, newly confirmed transfers leave the pool and detached transfers return, unless their nonce has already been consumed by the winning branch. Both balances and nonces follow the selected branch. Pending transfers restored after a reorganization may need pending incoming funds before they become mineable.

A received block is validated against the recipient's own parent state. Invalid transaction sequences or oversized payloads are rejected before forwarding. Blocks whose parents are missing wait in a deduplicated orphan buffer and are processed when the parent arrives. Reconnecting a directed link sends the source's public history parent-first; unpublished blocks remain private. Link changes apply to future transmissions, so already-sent messages can arrive after disconnection.

送信者ごとに`0`から始まるnonce（連番）を管理します。残高が足り、nonceが期待値と一致する取引のみ採用します。同じ送信者・nonceの競合取引は別の分岐上では成立しますが、同一チェーンでは両方を採用できません。チェーン再編時には残高・nonceが採用された分岐の状態に切り替わり、外れた取引をプールへ戻して再検証します。

## Java API

```java
// Automatically assigned sender nonce; deterministic identifier from the transaction RNG.
Transaction payment = simulation.createTransaction("0", "1", 5);

// Explicit nonce for a branch-specific or conflicting-spend experiment.
Transaction competitor = simulation.createTransaction("0", "1", 7, 0L);

Node sender = simulation.getNetwork().getNodeList().get(0);
simulation.getScheduler().addNewEvent(new SendTransaction(0, sender, payment));
LedgerState state = sender.getBlockchain().getLatestBlock().getLedgerState();
double balance = state.getBalance("0");
long nextNonce = state.getNextNonce("0");
```

The original four-argument `Transaction(from, to, value, hash)` constructor remains available and assigns nonce `0`; use the five-argument constructor `Transaction(from, to, value, nonce, hash)` for subsequent transfers. The compatibility constructor accepts zero-value transfers. `LedgerState` is immutable and returns defensive copies of balance and nonce arrays. Block hashes and transaction hashes are deterministic simulation identifiers; they do not establish cryptographic validity.

Final canonical balances/nonces and transaction counts should be read from a chosen observer's canonical tip. Nodes may disagree during a partition or at the simulation horizon; the aggregate chain is an event-recording view and should not be treated as universal consensus.
