package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.HashGenerator;

public class PoS extends Consensus{

	
	
	private double share;
	
	public PoS(Node node, double share) {
		super(node);
		this.share = share;
	}
	
	public Block generateBlock(Block previousBlock, double startTime) {
		String previoushash = previousBlock.getHash();
		
		
		String randomString =String.valueOf(Math.random());
		String hash = HashGenerator.generateHash(randomString + previoushash);
		
		
		double timestamp = startTime + this.blocktime();
		
		
		return new Block(hash, previousBlock, timestamp, this.node,null);
	}
	
	public Block generateBlock(Block previousBlock) {
		String previoushash = previousBlock.getHash();
		
		String randomString =String.valueOf(Math.random());
		String hash = HashGenerator.generateHash(randomString + previoushash);
		
		
		double timestamp = previousBlock.getTimestamp() + this.blocktime();
		
		
		return new Block(hash, previousBlock, timestamp, this.node, null);
	}
	
	
	public double blocktime() {
		double time = Configuration.BLOCK_INTERVAL / this.share;
		return time;
	}
	
	
	
	
	
	
}
