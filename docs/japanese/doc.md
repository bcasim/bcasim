# BCASim の実行と拡張

## ビルドと実行

JDK 17 と Maven 3 を使用して検証しています。Java のソース／ターゲットは 8 を維持しています。

```sh
mvn clean verify
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/selfish-mining.properties --output runs/selfish-1
```

`--seed 42`、`--duration 1000`で、設定ファイルのseedと実験時間を上書きできます。`--help`で引数を確認できます。従来の`java -cp target/bcasim-0.0.1-SNAPSHOT.jar jp.kota.bcasim.main.Main`も利用できます。

引数なしでは従来と同じ2ノード（0がSelfish Mining、1が通常ノード）、PoW、ハッシュレート0.48/0.52、実験時間100000で実行します。seedの既定値は1です。既存の出力を削除せず、`output-file`、`output-file-1`…の空いている名前を選びます。指定した`--output`が空でない場合はエラーになります。

## 設定ファイル

Javaの定数を書き換えて再コンパイルする必要はありません。UTF-8の`.properties`ファイルを指定します。未知のキー、不正な数値、ノード数の不一致、自己接続は実行前にエラーとして報告します。

| キー | 意味 |
| --- | --- |
| `seed` | 実験の乱数seed。採掘・識別子・トランザクション用の乱数列は分離 |
| `simulation.time` | 終了時刻。境界上のイベントは処理対象 |
| `consensus` | `PoW` または `PoS` |
| `block.interval` | ブロック生成間隔の基準値 |
| `block.size`, `block.reward` | ブロック容量・記録する報酬 |
| `transaction.size` | トランザクション容量 |
| `nodes.weights` | ノードごとの採掘能力／持分。カンマ区切り。0は採掘しないノード |
| `nodes.strategies` | `honest`, `selfish`, `double-spend`。ノードごとに指定 |
| `network.matrix` | 有向隣接行列。列はカンマ、行はセミコロンで区切る |
| `network.blockDelay`, `network.transactionDelay` | 伝送遅延 |
| `transaction.generate` | 自動生成を有効にする。既定は`true`。正のrateも必要 |
| `transaction.rate` | ネットワーク全体の単位時間あたり生成試行数。ポアソン到着。既定`0`で生成なし |
| `transaction.value` | 自動生成する送金の額。正の整数。既定`1` |
| `transaction.initialBalance` | 各ノード口座の初期残高。既定`1000` |
| `observer.node` | 指標の基準とする採用チェーンのノード番号。既定`0` |
| `network.changes` | `時刻:connect:送信元:送信先`または`時刻:disconnect:送信元:送信先`をセミコロン区切りで指定 |

ノード数は`nodes.weights`から決まり、戦略数と行列のサイズを一致させる必要があります。重みは自動で正規化しません。すべて0の構成は拒否します。通常、重みの合計を1として指定します。

[通常ノード](../../examples/honest.properties)、[Selfish Mining](../../examples/selfish-mining.properties)、[Double Spending](../../examples/double-spending.properties)、[簡易PoS](../../examples/proof-of-stake.properties)の例を用意しています。PoSは既存の「間隔÷持分」による簡易モデルです。実際のPoSプロトコルの検証・投票・ファイナリティはモデル化していません。

## 結果と再実行

- `event.json`：処理したイベントの記録。終了時刻より後のイベントは含まない。
- `block.json`：初めて受理したブロック。Genesisを含む。
- `0_blockchain.json` / `.csv`：ノード0のチェーン。
- `adjacencyMatrix.csv`：終了時点のネットワーク。
- `initialAdjacencyMatrix.csv`：初期ネットワーク。接続変更の再生に使用。
- `configuration.properties`：初期設定。次回の`--config`へそのまま渡せる。
- `configuration.txt`：人が読むための設定記録。
- `metrics.json`：イベント数・最終ノード情報・採用チェーンの指標・残高・未承認取引数。定義は後述。
- `Mainchain.txt`：ノード0の採用チェーンに含まれるノード別ブロック数。
- `attackLog.txt`：攻撃戦略のログ。

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config runs/selfish-1/configuration.properties --output runs/selfish-replay
```

同じバージョンの組み込み戦略で、同じ設定とseedを使うと同じ結果になります。設定した`network.changes`とトランザクションの条件も保存されます。Javaから注入した独自戦略・合意形成・フォーク選択・接続操作は、そのコードも再現する必要があります。

HTTPサーバーで可視化ツールを開き、`adjacencyMatrix.csv`、`block.json`、`event.json`をまとめて選択・ドロップできます。接続変更の実験では`initialAdjacencyMatrix.csv`、指標表示には`metrics.json`も含めます。時刻移動、イベント／ブロック単位の再生、詳細表示、別実験との指標比較に対応しています。従来どおり`output-file/`へ配置する方法も使えます。画面で作成した設定ファイルはダウンロードしてJava CLIで実行してください。サイト内デモへの同期方法は可視化リポジトリのREADMEを参照してください。

## 一括実験

`--batch 計画ファイル --output 出力先`で、seed、ノード0の戦略・採掘比率、ブロック遅延の組み合わせを実行します。`--parallel N`で並列数、`--resume`で再開を指定できます。実験ごとの再実行ファイルに加え、`summary.csv`と、グラフ・seed間の点ごとの95%信頼区間を含む`report.html`を生成します。再開時は設定の指紋と出力のチェックサムを検証します。[一括実験ガイド](../experiments-ja.md)と[計画例](../../examples/batch-comparison.properties)を参照してください。

## トランザクションと接続変更

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/transactions.properties --output runs/transactions
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/partition.properties --output runs/partition
```

口座IDはノード番号（`0`, `1`, …）です。送金は整数額と送信元の連番を持ち、二重使用・重複・残高不足を検証します。採掘報酬はブロック内の取引を処理した後で付与します。自動生成は別の口座を宛先とし、送信元に十分な残高がなければその回を見送ります。既定の`transaction.rate=0`では従来の取引なし実験の結果を維持します。署名・手数料・UTXO・店舗の承認待ち判定はモデル化していません。組み込み`double-spend`は私的分岐と公開分岐の競争であり、競合する2件の決済を自動作成する戦略ではありません。[台帳モデルの詳細](../transaction-model.md)も参照してください。

`network.changes=200:disconnect:0:1;600:connect:0:1`はノード0から1への有向接続を操作します。双方向の分断には逆方向も指定します。時刻は実験時間内に収め、同時刻の変更は記載順に実行します。切断後の新しい送信を止めますが、すでに送信済みのメッセージは到着します。再接続時は公開済みの不足ブロックを通常の遅延で受信側へ同期し、非公開のブロックは公開しません。親より先に着いたブロックは親を待ちます。取引プールの再接続時同期は行いません。

## 指標の定義

指標は終了時点のスナップショットで、Genesisを除きます。`observer.node`で基準チェーンを選びます。互換性のため、`0_blockchain.*`と`Mainchain.txt`は引き続きノード0の出力です。

| 指標 | 定義 |
| --- | --- |
| `acceptedBlocks`, `mainchainByMiner` | 観測ノードの採用チェーン上の総ブロック数・採掘者別内訳 |
| `totalPublishedBlocks` | 全体の公開ツリーで受理した一意のブロック数 |
| `staleBlocks`, `staleFraction` | 公開済みのうち観測ノードの採用チェーンにない数・公開数に対する比率 |
| `attackerRevenueShare` | 採用ブロックに占めるノード0の比率。手数料や金銭的損益は含まない |
| `forkPoints` | 観測ツリー内で受理済みの子を2つ以上持つ親の数 |
| `reorgCount`, `maxReorgDepth` | 採用先端の変更でブロックが外れた回数・一度に外れた最大数 |
| `remoteBlockReceipts`, `meanPropagationDelay` | 採掘者以外のノードによる一意のブロック受理数・初回公開受理から各受理までの平均時間 |
| `attackSuccesses`, `attackFailures`, `attackSuccessRate` | 全ノードの組み込みDouble Spending分岐競争で終了した試行数・成功数÷終了試行数 |
| `transactionsConfirmed` | 観測ノードの採用チェーン内の一意の取引数 |
| `canonicalBalances`, `canonicalNonces` | 観測ノードの採用先端における口座ごとの残高・次の連番。初期残高と採掘報酬を含む |
| `pendingTransactions`, `pendingOrphanBlocks`, `rejectedBlocks` | 各ノードのプール内取引数・親待ちブロック数・不正な受信ブロックの拒否回数 |

分母や観測数が0の場合、比率と平均はJSONの`null`です。伝播時間には分断や親待ちの時間も含みます。分断中の未採用ブロックは単に観測ノードに未到着の可能性があり、staleは永続的な無効判定を意味しません。従来どおり終了時に公開する私的ブロックも公開数に含めますが、その受信イベントは実行しません。比較時は実験時間・観測ノード・戦略などの条件を揃えてください。

## Java APIと拡張点

```java
SimulationConfig config = SimulationConfig.builder()
    .seed(42)
    .simulationTime(1000)
    .nodeStrategies("honest", "honest")
    .build();
SimulationResult result = new Simulation(config).run(); // ファイル出力なし
```

`Simulation`は1回の実行専用です。繰り返し・並列実験では実験ごとに新しいインスタンスを作成します。設定は不変で配列は防御的にコピーされます。時計、キュー、ネットワーク、チェーン、乱数、出力に実験をまたぐ可変static状態はありません。

- `NodeBehavior`：受信・採掘成功・初期化時の判断。`NodeBehaviorFactory`でノードごとに新しい戦略を作る。
- `ConsensusFactory` / `Consensus`：ブロック生成。実装は`getWeight()`と2つの`generateBlock()`を提供する。
- `ForkChoice`：受け入れた候補から採用先端を選ぶ。既定は最長チェーン、同じ高さなら自分のブロックを優先。
- `ResultWriter`：記録先。既定は`NOOP`。`FileResultWriter(Path)`でファイルへ出力する。

上記の拡張点は`Simulation(config, writer, consensusFactory, behaviorFactory, forkChoice)`に渡せます。独自実装が持つ可変状態も、実験間で共有しないでください。`Node`が伝播、採掘イベント管理、チェーン追加を担当し、戦略は判断に集中できます。

`Simulation.createTransaction()`は再現可能な識別子を生成します。直接`new Transaction(..., hash)`を使う場合は指定したhashをそのまま保持します。自動生成は独立した乱数列を使います。プールは残高と連番を検証し、容量内のスナップショットを返します。採用チェーンで承認された取引を除去し、チェーン切り替えで外れた有効な取引は戻します。競合取引や分岐ごとの連番には`createTransaction(from, to, value, nonce)`を使用します。3引数のメソッドは送信元ごとに連番を採番します。

## 移行と振る舞い

従来のstaticな`Scheduler`／`Network`呼び出しは`simulation.getScheduler()`／`simulation.getNetwork()`経由へ変更してください。`DefaultNode`／`Attacker3`／`Attacker4`の便宜コンストラクタは先頭に`Simulation`を受け取ります。旧`Configuration`は非推奨の定数として残っていますが、実行時は参照しません。旧`OutputResult`は`ResultWriter`に置き換えました。CSV入力を使っていた構成は、重みを`nodes.weights`、隣接行列を`network.matrix`に移してください。

イベントは同時刻なら登録順で処理し、ノードごとの未処理`FoundBlock`は新規登録で置き換えます。イベントオブジェクトは一度だけ登録でき、登録後のID変更は拒否します。再スケジュールには新しいイベントを作成してください。

終了時は従来の私的ブロックの最終公開を維持しています。公開は最後に処理した時刻で行い、それに伴う受信イベントは処理しません。このため、終了時点ですべてのノードが同じチェーンを持つとは限りません。ブロックの複製では親子関係・受信時刻を分離し、取引データと伝播済み集合を共有します。受理したブロックには親の状態から検証した不変の口座台帳を持たせます。従来の残高リストは台帳検証の基準には使いません。

構造整理に加えた明示的な修正は、終了時刻を超える未実行イベントの記録除外、設定されたPoSの選択、トランザクションプールの容量処理、指定トランザクションhashの保持、キャッシュの高さ検索、JSONのエスケープと配列生成です。ライブラリ内部の`System.exit(0)`は例外に置き換え、CLIのみ失敗時に非0で終了します。

## 検証

`mvn clean verify`で実行します。旧コードにseedを注入して採取した3シナリオの記録に対し、イベントID・時刻・順序・ハッシュを照合します。キューの差分比較、繰り返し／並列実験、設定検証、終了境界、出力互換性、再実行、CLIもテストします。旧記録の採取方法は`src/test/resources/legacy-traces.md`に記載しています。
