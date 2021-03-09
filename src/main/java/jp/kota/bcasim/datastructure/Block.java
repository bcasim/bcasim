package jp.kota.bcasim.datastructure;


import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.fileio.OutputResult;

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
	
	private double[] balanceList = new double[Configuration.NUMBER_OF_NODES];
	
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
		this.transactionList = transactionList;
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
		this.transactionList = transactionList;
		
		this.nextBlocks = new ArrayList<Block>();
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
		return transactionList;
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
		
		//ブロックを出力
		if(this.transmittedNodes.isEmpty()) {
			OutputResult.outBlockJson(this);
		}
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
