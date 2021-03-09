package jp.kota.bcasim.main.node;


import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.Scheduler;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.event.ReceiveTransaction;
import jp.kota.bcasim.main.node.consensus.Consensus;
import jp.kota.bcasim.main.node.consensus.PoW;
import jp.kota.bcasim.network.Network;
import jp.kota.bcasim.tool.fileio.OutputResult;

import java.util.ArrayList;

/**
 * selfish mining
 */

public abstract class Node {
	
	
	
	protected Blockchain blockchain;
	private String nodeID;
	private Consensus consensus;
	protected ArrayList<Block> unpublishedBlocks;
	private TransactionPool transactionPool;
	

	/**
	 * Genesisブロック生成時に利用する
	 */
	public Node(String genesis) {
		this.nodeID = genesis;
	}
	
	/**
	 * マイナーノード生成時に利用する
	 */
	public Node(String name, double hashrate) {
		this.blockchain = new Blockchain(name,this);
		this.nodeID = name;
		this.consensus = new PoW(this, hashrate);
		this.unpublishedBlocks = new ArrayList<Block>();
		this.transactionPool = new TransactionPool();
	}
	
	public double getHashrate() {
		PoW _pow =  (PoW)this.consensus;
		return _pow.getHashrate();
	}
	
	
	
	public abstract void initNode(Event event);
	
	public abstract void receiveBlock(Event event);
	
	public abstract void foundBlock(Event event);
	
	public abstract void receiveTransaction(Event event);
	
	public abstract void sendTransaction(Event event);
	
	//public abstract void propagateTransaction(Event event);
	
	
	//---------------------イベントの定義-----------------------------
	//内生事象
	//public abstract void PublishBlock();
	//内生事象
	//public abstract void PropagateBlock(Block block);
	/**
	 * 到着したブロックの伝送
	 */
	public void PropagateBlock(Block block) {
		ArrayList<Node> nodeList =  Network.getAdjacencyNode(this.getName());
		double delay = Network.getBlockDelay();
		Block newBlock = block;
		
		for(int i = 0; i < nodeList.size(); i++) {
			Node destinationNode = nodeList.get(i);
			
			if(block.verifyNode(destinationNode)) {
				continue;
			}		
			
			Event newEvent = new ReceiveBlock(Scheduler.getSimulationTime() + delay,destinationNode,newBlock,this);
			Scheduler.addNewEvent(newEvent);
			
		}	
	}
	
	public void PropagateTransaction(Transaction transaction) {
		ArrayList<Node> nodeList =  Network.getAdjacencyNode(this.getName());
		double delay = Network.getTransactionDelay();
		Transaction newTransaction = transaction;
		
		for(int i = 0; i < nodeList.size(); i++) {
			Node destinationNode = nodeList.get(i);
			
			if(transaction.verifyNode(destinationNode)) {
				continue;
			}		
			Event newEvent = new ReceiveTransaction(Scheduler.getSimulationTime() + delay,destinationNode,newTransaction);
			Scheduler.addNewEvent(newEvent);
			
		}	
	}
	
	/**
	 * 未公開ブロックの公開
	 */
	public void PublishBlock() {
		ArrayList<Node> nodeList =  Network.getAdjacencyNode(this.getName());
		
		double delay = Network.getBlockDelay();
		
		for(int j=0;this.unpublishedBlocks.size()>j;j++){
			Block newBlock = this.unpublishedBlocks.get(j);
			this.addNewBlock(newBlock);
			for(int i = 0; i < nodeList.size(); i++) {
				
				
				Node destinationNode = nodeList.get(i);
				
				
				//エラーチェック
				if(this.getName().equals(destinationNode.getName())) {
					System.out.println("Err自分に送信");
					System.exit(0);
				}
				
				//newBlock.setArriveBlockTime(Scheduler.getSimulationTime() + delay);
				Event newEvent = new ReceiveBlock(Scheduler.getSimulationTime() + delay,destinationNode,newBlock,this);
				Scheduler.addNewEvent(newEvent);
			}
		}
		this.unpublishedBlocks = new ArrayList<Block>();
	}
	
	
	/**
	 * ブロックの生成開始
	 */
	public void StartMining(Block block) {
		double addBlockTime = block.getTimestamp();
		Event newEvent = new FoundBlock(addBlockTime,this,block);
		Scheduler.addNewEvent(newEvent);
	}
	/**
	 * ブロックの生成停止
	 */
	public void StopMining() {
		FoundBlock foundEvent = Scheduler.getFoundEvent(this);
		if(foundEvent!=null) {
			int eventID = foundEvent.getEventID();
			Scheduler.removeEvent(eventID);
		}
	}
	
	
	//--------------------------------------------------
	
	/**
	 * 生成開始時刻を指定してブロック生成
	 * receveBlockイベント発生自に使用
	 */
	public Block generateNewBlock(double startTime) {
		Block previousBlock = blockchain.getLatestBlock();
		if(unpublishedBlocks.size()!=0) {
			System.exit(0);
			if(unpublishedBlocks.get(unpublishedBlocks.size()-1).getHeight()>=previousBlock.getHeight()) {
				previousBlock = unpublishedBlocks.get(unpublishedBlocks.size()-1);
			}
			
		}
		Block nextBlock = this.consensus.generateBlock(previousBlock,startTime);
		
		return nextBlock;
	}
	
	/**
	 * 新規ブロックの作成
	 */
	public Block generateNewBlock() {
		Block previousBlock = blockchain.getLatestBlock();
		
		if(unpublishedBlocks.size()!=0) {
			System.exit(0);
			if(unpublishedBlocks.get(unpublishedBlocks.size()-1).getHeight()>=previousBlock.getHeight()) {
				previousBlock = unpublishedBlocks.get(unpublishedBlocks.size()-1);
				
			}
		}
		Block nextBlock = this.consensus.generateBlock(previousBlock);
		return nextBlock;
	}
	
	/**
	 * 親ブロックを指定してブロックを生成
	 */
	public Block generateNewBlock(Block previousBlock) {
		
		Block nextBlock = this.consensus.generateBlock(previousBlock);
		
		return nextBlock;
	}
	
	
	/**
	 * 自ノードのブロックチェーンに新規ブロックを追加する
	 */
	public void addNewBlock(Block block) {
		
		
		Scheduler.addBlock(block);
		
		Block cloneBlock = Block.cloneBlock(block);
		this.blockchain.addBlock(cloneBlock);
	}
	
	/**
	 * トランザクションプールにトランザクションを追加
	 */
	public void addTransaction(Transaction transaction) {
		this.transactionPool.addNewTransaction(transaction);
	}
	/**
	 * トランザクションを取得
	 */
	public Transaction geTransaction() {
		return this.transactionPool.popTransaction();
	}
	
	
	public Blockchain getBlockchain() {
		return this.blockchain;
	}
	
	public String getName() {
		return this.nodeID;
	}
	
	/**
	 * Localchainに新規ブロックを追加
	 */
	public void addUnpublishedBlocks(Block block) {
		this.unpublishedBlocks.add(block);
	}
	
	/**
	 * Localchainの最長ブロック高を返す
	 */
	public int getHeightunpublishedBlock() {
		int height = -1;
		if(this.unpublishedBlocks.size()!=0) {
			height = this.unpublishedBlocks.get(0).getHeight();
		}
		return height;
	}
	
	/**
	 * 未公開を初期化する
	 */
	public void clearUnpublishedBlocks() {
		this.unpublishedBlocks = new ArrayList<Block>();
	}

	
	/**
	 * 公開チェーンと非公開チェーンの差を調べる
	 * vertex と edge
	 */
	public int getDifferenceLen(Block vertex) {
		Block latestBlock = this.blockchain.getLatestBlock();
		int local = 0;
		if(vertex != null) {
			local = vertex.getHeight();
		}
		if(this.unpublishedBlocks.size()!=0) {
			Block unpublishedBlock = this.unpublishedBlocks.get(this.unpublishedBlocks.size()-1);
			local = unpublishedBlock.getHeight();
		}
		return local - latestBlock.getHeight();
	}
	/**
	 * Publicチェーンの分岐からの長さを返す
	 */
	public int getPublicBranch(Block vertex) {
		
		int local = vertex.getHeight();
		if(this.unpublishedBlocks.size()!=0) {
			Block unpublishedBlock = this.unpublishedBlocks.get(this.unpublishedBlocks.size()-1);
			local = unpublishedBlock.getHeight();
		}
		return local - vertex.getHeight();
	}
	
	/**
	 * Privateチェーンの分岐からの長さを返す
	 */
	public int getPrivateBranch(Block vertex) {
		Block latestBlock = this.blockchain.getLatestBlock();
		return latestBlock.getHeight() - vertex.getHeight();
	}
	
	/**
	 * ログを出力
	 */
	public void outputLog(String log) {
		OutputResult.outAttackLog(log);
	}
	
	public TransactionPool getTransactionPool() {
		return this.transactionPool;
	}
	
	
}
