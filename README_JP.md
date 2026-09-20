[English](https://github.com/bcasim/bcasim/blob/main/README.md) | [日本語](https://github.com/bcasim/bcasim/blob/main/README_JP.md)

<div align="center"><img src="https://github.com/bcasim/bcasim/blob/main/pic/logo.png" width="400"/></div>

# BCASim : Blockchain Attack Simulator

 BCASimはオープンソースのブロックチェーンシミュレータです。利用者はノードの動作シナリオとプロトコル特性を柔軟にカスタマイズすることができます。このカスタマイズによって、Double spending attack, Selfish mining, Sybil Attackなどを扱うことができます。
 <br>
 <br>
<div align="center"><img src="https://github.com/bcasim/bcasim/blob/main/pic/large-network.gif" width="90%"/></div>
<br>

# 特徴
* ノードの動作シナリオのカスタマイズ機能。
* 攻撃の再現に必要なブロックチェーンのデータ構造の再現。
* 合意形成や難易度調節、フォークチョイスルールなどの追加。
* ネットワーク構造とブロックチェーン構造の可視化。

## クイックスタート
```
$ git clone https://github.com/bcasim/bcasim
$ cd bcasim
$ mvn clean verify
$ java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/selfish-mining.properties
```

設定ファイルとseedを指定して、再現可能な実験を実行できます。`--output`で出力先を指定でき、既存の結果は上書きしません。出力の`configuration.properties`を`--config`へ渡すと再実行できます。JDK 17 / Maven 3で検証しています。

```sh
java -jar target/bcasim-0.0.1-SNAPSHOT.jar --config examples/honest.properties --seed 42 --output runs/honest-42
```

通常ノード・Selfish Mining・Double Spending・簡易PoSの設定例を`examples/`に用意しています。[実行・拡張・移行ガイド](docs/japanese/doc.md)を参照してください。

## 利用方法の詳細

詳細はドキュメントを確認ください。
<br>
シミュレーションパラメータや攻撃シナリオのカスタマイズ方法についてまとめています。
<br>
<br>
・[English document](https://github.com/bcasim/bcasim/blob/main/docs/english/doc.md)
 <br>
・[日本語ドキュメント](https://github.com/bcasim/bcasim/blob/main/docs/japanese/doc.md)


## 実験例
以下はシミュレータを利用して行った実験の例です。画像の実線は理論値、バツはシミュレーション結果をプロットしています。左はDouble Spending Attackの攻撃成功確率を対象にした実験です。右はSelfish Miningノードのブロック生成割合を対象にした実験です。

|Double Spending Attack|Selfish Mining|
|---|---|
|![](https://github.com/bcasim/bcasim/blob/main/pic/plot1.png)|![](https://github.com/bcasim/bcasim/blob/main/pic/plot2.png)|

## デモ動画
BCASimのシミュレーション結果を可視化したデモです。このデモは関連プロジェクトである[BCASim Visualization](https://github.com/bcasim/bcasim-visualization)ツールによって実現されています。
<br>
1. Large Network - [gif](https://github.com/bcasim/bcasim/blob/main/demo/large-network.md)
2. Selfish Mining - [gif](https://github.com/bcasim/bcasim/blob/main/demo/selfish-mining.md)
3. Double Spending Attack - [gif](https://github.com/bcasim/bcasim)
4. Sybil Attack - [gif](https://github.com/bcasim/bcasim)

## BCASimの関連プロジェクト
* [BCASim](https://github.com/bcasim/bcasim) - 攻撃分析のためのブロックチェーンシミュレータ
* [BCASim Visualization](https://github.com/bcasim/bcasim-visualization) - BCASimのシミュレーション結果の可視化ツール

## 著者
質問や意見などがございましたらこちらまでお気軽にご連絡ください。

* bigmakiinum@outlook.jp
* https://github.com/resoto

## ライセンス
* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
