package jp.kota.bcasim.main.event;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.node.Node;


public class ReceiveBlock extends Event{
	
	
	
	private Block block;
	private Node from;
	
	
	public ReceiveBlock(double eventTime,Node node,Block block,Node from) {
		super(eventTime,node);
		this.block = block;
		this.from = from;
	}
	
	
	
	public void process() {
		super.node.receiveBlock(this);
	}
	
	
	public Block getBlock() {
		return this.block;
	}
	
	public Node getFrom() {
		return this.from;
	}
	

}
