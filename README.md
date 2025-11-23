[English](https://github.com/bcasim/bcasim/blob/main/README.md) | [日本語](https://github.com/bcasim/bcasim/blob/main/README_JP.md)

<div align="center"><img src="https://github.com/bcasim/bcasim/blob/main/pic/logo.png" width="400"/></div>

# BCASim: Blockchain Attack Simulator

BCASim is an open-source blockchain simulator for attack analysis. Users can freely customize node operations and protocol specifications to simulate various scenarios, such as Double Spending Attacks, Selfish Mining, and Sybil Attacks.
<br>
<br>
<div align="center"><img src="https://github.com/bcasim/bcasim/blob/main/pic/large-network.gif" width="90%"/></div>
<br>

## Main Features
* **Customizable Scenarios:** Flexible configuration of node operation scenarios.
* **Realistic Data Structure:** Replicates the blockchain data structure required to simulate attacks.
* **Protocol Implementation:** Includes consensus building, difficulty adjustment, and fork choice rules.
* **Visualization:** Visualizes both the network topology and the blockchain structure.

## Quick Start

### Prerequisites
* Java (version 1.7+)
* Apache Maven (version 3.6.3)

### Installation & Usage

```bash
$ git clone https://github.com/bcasim/bcasim
$ cd bcasim
$ mvn clean install
$ mvn compile
$ java -cp target/bcasim-0.0.1-SNAPSHOT.jar src/main/java/jp/ac/jaist/bcasim/main/Main.java
```

## For more detail

See the usage document page.
<br>
It summarizes how to customize simulation parameters and attack scenarios.
<br>
* [English document](https://github.com/bcasim/bcasim/blob/main/docs/english/doc.md)
* [日本語ドキュメント](https://github.com/bcasim/bcasim/blob/main/docs/japanese/doc.md)

## Experimental example
The following is an example of an experiment conducted using a simulator. The solid line in the image plots the theoretical value, and the cross plots the simulation result. The left is an experiment targeting the attack success probability of Double Spending Attack. The right is an experiment targeting the block generation ratio of the Selfish Mining node.


|Double Spending Attack|Selfish Mining|
|---|---|
|![](https://github.com/bcasim/bcasim/blob/main/pic/plot1.png)|![](https://github.com/bcasim/bcasim/blob/main/pic/plot2.png)|

## Demo
This is a demo that visualizes the simulation results of BCASim.
<br>
1. Large Network - [gif](https://github.com/bcasim/bcasim/blob/main/demo/large-network.md) 
2. Selfish Mining - [gif](https://github.com/bcasim/bcasim/blob/main/demo/selfish-mining.md)
3. Double Spending Attack - [gif](https://github.com/bcasim/bcasim)
4. Sybil Attack - [gif](https://github.com/bcasim/bcasim)

## Other BCASim projects

* [BCASim](https://github.com/bcasim/bcasim) - Open source blockchain simulator for attack analysis
* [BCASim Visualization](https://github.com/bcasim/bcasim-visualization) - Simulation result visualization tool for BCASim

## Authors
If you have any questions or comments, please feel free to contact us.
* bigmakiinum@outlook.jp
* http://www.jaist.ac.jp/is/labs/aoki-lab/index-e.html

## License
BCASim is released under the terms of the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
<br>
Please refer to the [COPYING](https://github.com/bcasim/bcasim/blob/main/LICENSE) file.

<div align="center">
  <img src="https://komarev.com/ghpvc/?username=bcasim&style=flat-square&color=blue" alt="visitors" />
</div>
