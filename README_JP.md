
# BCASim : Blockchain Attack Simulator 

 BCASimはオープンソースのブロックチェーンシミュレータです。利用者はノードの動作シナリオとプロトコル特性を柔軟にカスタマイズすることができます。このカスタマイズによって、Double spending attack, Selfish mining, Sybil Attackなどを扱うことができます。
 <br>
 <br>
・[English page](https://github.com/BCASim/BCASim/)
<br>
<br>
![](https://github.com/BCASim/BCASim/blob/main/pic/sample.gif)
<br>

## クイックスタート
```
$ git clone https://github.com/BCASim/bcasim
$ cd bcasim
$ mvn clean install
$ mvn compile
$ java -cp target/bcasim-0.0.1-SNAPSHOT.jar src/main/java/jp/kota/bcasim/Main.java
```

## 利用方法の詳細

詳細はドキュメントを確認ください。
<br>
シミュレーションパラメータや攻撃シナリオのカスタマイズ方法についてまとめています。
<br>
<br>
・[English document](https://github.com/BCASim/BCASim/blob/main/docs/english/doc.md)
 <br>
・[日本語ドキュメント](https://github.com/BCASim/BCASim/blob/main/docs/japanese/doc.md)



## 実験例
以下はシミュレータを利用して行った実験の例です。画像の実線は理論値、バツはシミュレーション結果をプロットしています。左はDouble Spending Attackの攻撃成功確率を対象にした実験です。右はSelfish Miningノードのブロック生成割合を対象にした実験です。

|Double Spending Attack|Selfish Mining|
|---|---|
|![](https://github.com/BCASim/BCASim/blob/main/pic/plot1.png)|![](https://github.com/BCASim/BCASim/blob/main/pic/plot2.png)|

## デモ動画
BCASimのシミュレーション結果を可視化したデモです。
<br>
<br>
1.・[Large network](https://github.com/BCASim/BCASim/blob/main/demo/LargeNetwork.md)
<br>
2.・[Selfish Mining](https://github.com/BCASim/BCASim/blob/main/demo/SelfishMining.md)

