
# BCASim : Blockchain Attack Simulator 


 BCASim is open sorce blockchain simulator for attack analysis. The user can freely change the node operation and protocol specifications. For example Double spending attack, Selfish mining, Sybil Attack.
<br>
<br>
・[日本語ページ](https://github.com/BCASim/BCASim/blob/main/README_JP.md)
<br>
<br>
![](https://github.com/BCASim/BCASim/blob/main/pic/sample.gif)

<br>

## Quick Start
```
$ git clone https://github.com/BCASim/bcasim
$ cd bcasim
$ mvn clean install
$ mvn compile
$ java -cp target/bcasim-0.0.1-SNAPSHOT.jar src/main/java/jp/kota/bcasim/main/Main.java
```

## For more detail

See the usage document page.
<br>
It summarizes how to customize simulation parameters and attack scenarios.
<br>
<br>
In preparation
<br>

## Experimental example
The following is an example of an experiment conducted using a simulator. The solid line in the image plots the theoretical value, and the cross plots the simulation result. The left is an experiment targeting the attack success probability of Double Spending Attack. The right is an experiment targeting the block generation ratio of the Selfish Mining node.


|Double Spending Attack|Selfish Mining|
|---|---|
|![](https://github.com/BCASim/BCASim/blob/main/pic/plot1.png)|![](https://github.com/BCASim/BCASim/blob/main/pic/plot2.png)|

## Demo video
This is a demo that visualizes the simulation results of BCASim.
<br>
<br>
1.・[Large network](https://github.com/BCASim/BCASim/blob/main/demo/LargeNetwork.md)
<br>
2.・[Selfish Mining](https://github.com/BCASim/BCASim/blob/main/demo/SelfishMining.md)



