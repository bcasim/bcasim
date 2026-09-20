package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.datastructure.Block;

@FunctionalInterface
public interface ForkChoice {
    Block select(String chainId, Block current, Block candidate);
}
