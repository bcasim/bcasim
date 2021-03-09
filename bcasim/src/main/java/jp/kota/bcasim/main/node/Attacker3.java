package jp.kota.bcasim.main.node;



import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;

public class Attacker3 extends Node{

	
	public Attacker3(String name,double hashrate) {
		super(name, hashrate);
	}
	
	
	Block targetBlock;
	int comfirmation = 6;
	int fail = 0;
	int succ = 0;
	/**
	 * 新規ブロックが到着した場合の動作
	 * 1.到着したブロックをpublic chainに追加する
	 * 2.攻撃の失敗条件を満たしているかを確認する
	 * 失敗していた場合は次の攻撃を開始する
	 */
	public void print_rate() {
		System.out.println("fail:"+fail);
		System.out.println("succ:"+succ);
		System.out.println("rate:"+(double)succ/(succ+fail));
	}
	
	
	/**
	 * ・ノードに新規ブロックを追加する
	 * ・新規ブロック生成イベントを開始（Foundイベント）
	 * ・unpublishイベントの初期化
	 */
	public void receiveBlockScenario(Block block,Node node) {
		node.addNewBlock(block);
		if(targetBlock==null) {
			targetBlock = block;
			node.StartMining(node.generateNewBlock(targetBlock));
		}
		//諦める
		if(super.getDifferenceLen(targetBlock) < -50) {
			fail++;
			super.outputLog("fail");
			node.clearUnpublishedBlocks();
			targetBlock = null;
			targetBlock = super.blockchain.getLatestBlock();
			node.StartMining(node.generateNewBlock(targetBlock));
		}
	}
	
	public void foundBlockScenario(Block block,Node node) {
		node.addUnpublishedBlocks(block);
		
		if(node.getPublicBranch(targetBlock) >= comfirmation && node.getDifferenceLen(targetBlock) >= 0) {
			succ++;
			node.outputLog("succ");
			node.PublishBlock();
			
			targetBlock = null;
			targetBlock = node.blockchain.getLatestBlock();
			node.StartMining(node.generateNewBlock(targetBlock));
			
		//攻撃が成功条件を満たさない場合
		}else {
			node.StartMining(node.generateNewBlock(block));			
		}
	}
	
	
	public void receiveBlock(Event event) {
		ReceiveBlock receiveBlock = (ReceiveBlock)event;
		this.receiveBlockScenario(receiveBlock.getBlock(), receiveBlock.getNode());
	}
	
	public void foundBlock(Event event) {
		FoundBlock foundBlock = (FoundBlock)event;
		this.foundBlockScenario(foundBlock.getBlock(), foundBlock.getNode());
	}
	
	
	public void initNode(Event event) {}
	
	public void receiveTransaction(Event event) {}

	
	public void sendTransaction(Event event) {}
	
}
