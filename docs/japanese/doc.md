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
| `seed` | 実験の乱数seed。採掘時間と識別子用の乱数列は分離 |
| `simulation.time` | 終了時刻。境界上のイベントは処理対象 |
| `consensus` | `PoW` または `PoS` |
| `block.interval` | ブロック生成間隔の基準値 |
| `block.size`, `block.reward` | ブロック容量・記録する報酬 |
| `transaction.size` | トランザクション容量 |
| `nodes.weights` | ノードごとの採掘能力／持分。カンマ区切り。0は採掘しないノード |
| `nodes.strategies` | `honest`, `selfish`, `double-spend`。ノードごとに指定 |
| `network.matrix` | 有向隣接行列。列はカンマ、行はセミコロンで区切る |
| `network.blockDelay`, `network.transactionDelay` | 伝送遅延 |
| `transaction.generate` | 従来設定の記録用。組み込みシナリオに自動トランザクション生成器は含まれない |

ノード数は`nodes.weights`から決まり、戦略数と行列のサイズを一致させる必要があります。重みは自動で正規化しません。すべて0の構成は拒否します。通常、重みの合計を1として指定します。

[通常ノード](../../examples/honest.properties)、[Selfish Mining](../../examples/selfish-mining.properties)、[Double Spending](../../examples/double-spending.properties)、[簡易PoS](../../examples/proof-of-stake.properties)の例を用意しています。PoSは既存の「間隔÷持分」による簡易モデルです。実際のPoSプロトコルの検証・投票・ファイナリティはモデル化していません。

## 結果と再実行

- `event.json`：処理したイベントの記録。終了時刻より後のイベントは含まない。
- `block.json`：初めて受理したブロック。Genesisを含む。
- `0_blockchain.json` / `.csv`：ノード0のチェーン。
- `adjacencyMatrix.csv`：終了時点のネットワーク。
- `configuration.properties`：初期設定。次回の`--config`へそのまま渡せる。
- `configuration.txt`：人が読むための設定記録。
- `metrics.json`：イベント数・ノードごとのチェーン高・最終ノード情報。
- `Mainchain.txt`：ノード0の採用チェーンに含まれるノード別ブロック数。
- `attackLog.txt`：攻撃戦略のログ。

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config runs/selfish-1/configuration.properties --output runs/selfish-replay
```

同じバージョンの組み込み戦略で、同じ設定とseedを使うと同じ結果になります。独自戦略・合意形成・フォーク選択や実行中のネットワーク変更を注入した実験では、そのコードと操作も再現する必要があります。設定ファイルだけには保存されません。

可視化するには出力の`adjacencyMatrix.csv`、`block.json`、`event.json`を`bcasim-visualization/output-file/`へ配置してHTTPサーバーで開きます。可視化の正本は`bcasim-visualization`で、サイト内デモへの同期方法は同リポジトリのREADMEを参照してください。

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

`Simulation.createTransaction()`は再現可能な識別子を生成します。直接`new Transaction(..., hash)`を使う場合は指定したhashをそのまま保持します。トランザクションの生成頻度・受信処理・検証は独自`NodeBehavior`で実装します。プールは容量内のスナップショットを返し、取り出しによる消費や残高検証は行いません。

## 移行と振る舞い

従来のstaticな`Scheduler`／`Network`呼び出しは`simulation.getScheduler()`／`simulation.getNetwork()`経由へ変更してください。`DefaultNode`／`Attacker3`／`Attacker4`の便宜コンストラクタは先頭に`Simulation`を受け取ります。旧`Configuration`は非推奨の定数として残っていますが、実行時は参照しません。旧`OutputResult`は`ResultWriter`に置き換えました。CSV入力を使っていた構成は、重みを`nodes.weights`、隣接行列を`network.matrix`に移してください。

イベントは同時刻なら登録順で処理し、ノードごとの未処理`FoundBlock`は新規登録で置き換えます。イベントオブジェクトは一度だけ登録でき、登録後のID変更は拒否します。再スケジュールには新しいイベントを作成してください。

終了時は従来の私的ブロックの最終公開を維持しています。公開は最後に処理した時刻で行い、それに伴う受信イベントは処理しません。このため、終了時点ですべてのノードが同じチェーンを持つとは限りません。ブロックの複製では、チェーンの親子関係・受信時刻を分離し、伝播済み集合・トランザクション・残高の共有は従来どおり保持します。

構造整理に加えた明示的な修正は、終了時刻を超える未実行イベントの記録除外、設定されたPoSの選択、トランザクションプールの容量処理、指定トランザクションhashの保持、キャッシュの高さ検索、JSONのエスケープと配列生成です。ライブラリ内部の`System.exit(0)`は例外に置き換え、CLIのみ失敗時に非0で終了します。

## 検証

`mvn clean verify`で実行します。旧コードにseedを注入して採取した3シナリオの記録に対し、イベントID・時刻・順序・ハッシュを照合します。キューの差分比較、繰り返し／並列実験、設定検証、終了境界、出力互換性、再実行、CLIもテストします。旧記録の採取方法は`src/test/resources/legacy-traces.md`に記載しています。
