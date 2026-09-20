package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.datastructure.Block;

/** Preserve the original longest-chain rule: on ties prefer the node's own block. */
public final class LongestChain implements ForkChoice {
    public Block select(String chainId, Block current, Block candidate) {
        if (candidate.getHeight() > current.getHeight() ||
            (candidate.getHeight() == current.getHeight() && chainId.equals(candidate.getMiner().getName()))) return candidate;
        return current;
    }
}
