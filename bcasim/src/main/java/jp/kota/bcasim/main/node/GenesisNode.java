package jp.kota.bcasim.main.node;

import jp.kota.bcasim.main.event.Event;

public class GenesisNode extends Node{
	
	private static Node genesisNode = new GenesisNode();
	
	public GenesisNode() {
		super("Genesis",0);
	}
	
	public static Node getInstance() {
		return GenesisNode.genesisNode;
	}
	
	public void initNode(Event event) {}
	
	public void receiveBlock(Event event) {}
	
	public void foundBlock(Event event) {}
	
	public void receiveTransaction(Event event) {}
	
	public void sendTransaction(Event event) {}
	

}


