package jp.kota.bcasim.main.event;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.node.Node;

public class FoundBlock extends Event{
	
	

	
	private Block block;
	
	
	public FoundBlock(double eventTime,Node node,Block block) {
		super(eventTime,node);
		this.block = block;
	}
	
	
	public void process() {
		super.node.foundBlock(this);
		
	}
	
	
	public Block getBlock() {
		return this.block;
	}

}
