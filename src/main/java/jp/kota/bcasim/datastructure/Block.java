package jp.kota.bcasim.datastructure;


import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.transaction.LedgerState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Block implements Cloneable{
	
	
	private String hash;
	private String previousHash;
	private double timestamp;
	private int height;
	private Node miner;
	private ArrayList<Transaction> transactionList;
	
	
	private Block previousBlock;
	
	private ArrayList<Block> nextBlocks;
	
	private double receiveBlockTime;
	
	private double[] balanceList;
	private LedgerState ledgerState;
	
	private Set<Node> transmittedNodes = new HashSet<>();
	
	
	/**
	 * genesisブロックの生成のみに利用
	 */
	public Block(
			String hash,
			String previousHash,
			double timestamp,
			int height,
			Node miner,
			ArrayList<Transaction> transactionList) {
			
		this.hash = hash;
		this.previousHash = previousHash;
		this.timestamp = timestamp;
		this.height = height;
		this.miner = miner;
		this.transactionList = transactionList == null ? new ArrayList<Transaction>() : new ArrayList<>(transactionList);
        this.balanceList = new double[miner == null ? 0 : miner.getSimulation().getConfig().getNumberOfNodes()];
		this.nextBlocks = new ArrayList<Block>();
	}
	
	
	public Block(
			String hash,
			Block previousBlock,
			double timestamp,
			Node miner,
			ArrayList<Transaction> transactionList) {
		
		this.hash = hash;
		this.previousHash = previousBlock.getHash();
		this.timestamp = timestamp;
		this.height = previousBlock.getHeight() + 1;
		this.miner = miner;
		this.transactionList = transactionList == null ? new ArrayList<Transaction>() : new ArrayList<>(transactionList);
        this.balanceList = new double[miner == null ? 0 : miner.getSimulation().getConfig().getNumberOfNodes()];
		
		this.nextBlocks = new ArrayList<Block>();
		if (previousBlock.getLedgerState() != null && miner != null)
			setLedgerState(previousBlock.getLedgerState().applyBlock(this.transactionList,
				miner.getName(), miner.getSimulation().getConfig().getBlockReward()));
	}
	
	
	
	public void addNextBlock(Block block) {
		this.nextBlocks.add(block);
	}
	
	public ArrayList<Block> getNextBlocks() {
		return nextBlocks;
	}
	
	public void setPreviousBlock(Block previousBlock) {
		this.previousBlock = previousBlock;
	}
	
	public Block getPreviousBlock() {
		return this.previousBlock;
	}
	
	public void setReceiveBlockTime(double time) {
		this.receiveBlockTime = time;
	}
	
	public double getReceiveBlockTime() {
		return this.receiveBlockTime;
	}
	
	
	
	
	public String getHash() {
		return this.hash;
	}
	
	public String getPreviousHash() {
		return this.previousHash;
	}
	
	
	public double getTimestamp() {
		return this.timestamp;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public Node getMiner() {
		return this.miner;
	}
	
	public ArrayList<Transaction> getTransactionList(){
		return new ArrayList<>(transactionList);
	}

	public LedgerState getLedgerState() { return ledgerState; }
	public void setLedgerState(LedgerState state) {
		ledgerState = state;
		if (state != null) balanceList = state.getBalances();
	}
	
	public double[] getBalanceList() {
		return this.balanceList;
	}
	
	public void setBalanceList(double[] balanceList) {
		this.balanceList = balanceList;
	}
	
	public void print() {
		System.out.println("{");
		System.out.println("  ”hash” : " + this.hash + ",");
		System.out.println("  ”previousHash” : " + this.previousHash + ",");
		System.out.println("  ”timestamp” : " + this.timestamp + ",");
		System.out.println("  ”height;” : " + this.height + ",");
		System.out.println("  ”miner” : " + this.miner.getName() + ",");
		System.out.println("}");
	}
	
	/** Clone chain-local links/receipt time while sharing transaction, balance and propagation state.
     * This sharing preserves the original network duplicate suppression semantics.
     */
    public static Block cloneBlock(Block block) {
		if(block == null) {
			return null;
		}
		Block cloneBlock = new Block(
				block.getHash(),
				block.getPreviousHash(),
				block.getTimestamp(),
				block.getHeight(),
				block.getMiner(),
				block.getTransactionList());
		
		cloneBlock.ledgerState = block.ledgerState;
		cloneBlock.setBalanceList(block.getBalanceList());
		cloneBlock.setTransmittedNodes(block.getTransmittedNodes());
		return cloneBlock;
	}
	
	/*
	 * ブロックが伝送済みノードを記録
	 */
	public Set<Node> getTransmittedNodes(){
		return this.transmittedNodes;
	}
	
	public void addTransmittedNodes(Node node) {
		
		//電装済みノードに追加
		this.transmittedNodes.add(node);
	}
	
	
	public void setTransmittedNodes(Set<Node> transmittedNodes) {
		this.transmittedNodes = transmittedNodes;
	}
	
	
	
	public boolean verifyNode(Node node) {
		return this.transmittedNodes.contains(node);
	}
	
}
