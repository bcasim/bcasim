package jp.kota.bcasim.main.node;



import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;


public class Attacker4 extends Node{
	
	public Attacker4(String name,double hashrate) {
		super(name, hashrate);
	}
	
	/**
	 * ノードにブロックが到着した場合の処理
	 * リファレンス実装の処理を想定
	 * 1.到着ノードが生成した同じブロック高のブロックが存在する場合は取り消す
	 * ブロックが到着した時間から次のブロックが生成される時間でブロックを生成する
	 * 
	 * 2.その他の場合はブロックチェーンにブロックを追加する
	 */
	
	
	Block targetBlock;
	boolean stateZero = false;
	
	public void receiveBlockScenario(Block block, Node node) {
		int difference = node.getDifferenceLen(targetBlock);
		node.PropagateBlock(block);
		node.addNewBlock(block);
		
		if(stateZero) {
			stateZero = false;
			targetBlock = blockchain.getLatestBlock();
			node.StartMining(node.generateNewBlock());
		}
		
		if(difference==0) {
			node.PublishBlock();
			targetBlock = blockchain.getLatestBlock();
			node.StartMining(node.generateNewBlock());
			
		}else if(difference==1) {
			node.PublishBlock();
			stateZero = true;
		
		}else if(difference==2) {
			node.PublishBlock();
			targetBlock = blockchain.getLatestBlock();
			node.StartMining(node.generateNewBlock());
		}
	}
	
	public void foundBlockScenario(Block block, Node node) {
		int difference = getDifferenceLen(targetBlock);
		node.unpublishedBlocks.add(block);
		
		if(stateZero) {
			stateZero  = false;
			node.PublishBlock();
			targetBlock = blockchain.getLatestBlock();
		}
		
		//ブロックを公開する
		/*if(node.unpublishedBlocks.size()>=5000) {
			node.PublishBlock();
			targetBlock = blockchain.getLatestBlock();
		}*/
				
		//公開する
		if(difference ==0 && getPublicBranch(targetBlock)==2) {
			node.PublishBlock();
			targetBlock = blockchain.getLatestBlock();
		}
		node.StartMining(node.generateNewBlock(block));
	}

	public void initNodeScenario(Node node) {
		targetBlock = blockchain.getLatestBlock();
		node.StartMining(node.generateNewBlock());
	}
	
	
	public void initNode(Event event) {
		this.initNodeScenario(event.getNode());
	}
	
	public void receiveBlock(Event event) {
		ReceiveBlock arriveBlock = (ReceiveBlock)event;
		this.receiveBlockScenario(arriveBlock.getBlock(), arriveBlock.getNode());
	}
	
	public void foundBlock(Event event) {
		FoundBlock foundBlock = (FoundBlock)event;
		this.foundBlockScenario(foundBlock.getBlock(), foundBlock.getNode());
	}
	
	public void receiveTransaction(Event event) {}
	

	
	public void sendTransaction(Event event) {}

}