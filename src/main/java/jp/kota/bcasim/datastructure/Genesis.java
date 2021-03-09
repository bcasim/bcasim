package jp.kota.bcasim.datastructure;



import java.util.ArrayList;

import jp.kota.bcasim.main.node.GenesisNode;


public class Genesis {
	
	
	private static final String PREVIOUS_HASH = "none";
	private static final String GENESIS_HASH = "00000000";
	private static final double TIMESTAMP = 0;
	private static final int HEIGHT = 0;
	
	
	public static Block getGenesis() {
		
		
		ArrayList<Transaction> transactionList = new ArrayList<Transaction>();
		
		
		return new Block(
				Genesis.GENESIS_HASH, 
				Genesis.PREVIOUS_HASH, 
				Genesis.TIMESTAMP, 
				Genesis.HEIGHT, 
				GenesisNode.getInstance(),
				transactionList);
	}
	
	
}
