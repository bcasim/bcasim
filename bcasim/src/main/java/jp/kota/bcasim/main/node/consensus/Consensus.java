package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.node.Node;

public abstract class Consensus {
	
	
	protected Node node;
	
	public Consensus(Node node) {
		this.node = node;
	}
	
	
	public abstract Block generateBlock(Block previousBlock,double startTime);
	
	public abstract Block generateBlock(Block previousBlock);

	
}
