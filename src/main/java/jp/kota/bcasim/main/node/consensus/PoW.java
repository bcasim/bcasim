package jp.kota.bcasim.main.node.consensus;

import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.Scheduler;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.HashGenerator;



public class PoW extends Consensus{
	
	private double hashrate;
	
	public PoW(Node node, double hashrate) {
		super(node);
		this.hashrate = hashrate;
	}
	
	
	//時間を指定してブロックを生成
	public Block generateBlock(Block previousBlock, double startTime) {
		String previoushash = previousBlock.getHash();
		
		
		String randomString =String.valueOf(Math.random());
		String hash = HashGenerator.generateHash(randomString + previoushash);
		
		double timestamp = startTime + this.blocktime();
		
		Block newBlock = new Block(hash, previousBlock, timestamp, this.node,this.node.getTransactionPool().getTransactions());
		newBlock.setPreviousBlock(previousBlock);
		return newBlock;
	}
	
	//前のブロックを指定してブロックを生成
	public Block generateBlock(Block previousBlock) {
		
		String previoushash = previousBlock.getHash();
		
		
		String randomString =String.valueOf(Math.random());
		String hash = HashGenerator.generateHash(randomString + previoushash);
		
		
		
		double timestamp = Scheduler.getSimulationTime() + this.blocktime();
		
		
		
		Block newBlock = new Block(hash, previousBlock, timestamp, this.node,this.node.getTransactionPool().getTransactions());
		
		newBlock.setPreviousBlock(previousBlock);
		return newBlock;
		
	}
	
	private double blocktime() {
		double lambda=0;
		lambda = Configuration.BLOCK_INTERVAL / this.hashrate;
		double time = -1.0 * lambda * Math.log(1.0 - Math.random());
		return time;		
	}
	
	
	public void setHashrate(double hashrate) {
		this.hashrate = hashrate;
	}
	
	
	public double getHashrate() {
		return this.hashrate;
	}
}
