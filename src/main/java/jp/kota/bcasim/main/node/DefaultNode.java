package jp.kota.bcasim.main.node;


import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.main.Scheduler;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;



public class DefaultNode extends Node{
	
	
	

	/**
	 * 一般ノードを生成時に使用
	 */
	public DefaultNode(String name,double hashrate) {
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
	public void receiveBlock(Event event) {
		
		
		//新規に到着したイベント情報
		// arrive Block
		Node currentNode = event.getNode();
		ReceiveBlock eventCast = (ReceiveBlock)event;
		Block newBlock = eventCast.getBlock();
		int newBlockIndex = newBlock.getHeight();
		
		
		super.PropagateBlock(newBlock);
		
		//以前に登録されたイベント情報
		// Found Block
		FoundBlock foundEvent = Scheduler.getFoundEvent(currentNode);
		int foundEventBlockIndex = 0;
		if(foundEvent != null) {
			Block foundEventBlock = foundEvent.getBlock();
			foundEventBlockIndex = foundEventBlock.getHeight();
		}
		
		
		//以前に登録されたイベントが取り消される場合
		//他のノードがより長いブロックを発見した時
		if(newBlockIndex+1 >= foundEventBlockIndex) {
			//以前に登録されたイベントを削除
			if(super.unpublishedBlocks.size()>=2) {
				System.out.print(super.unpublishedBlocks.size());
				System.exit(0);
			}
			
			
			//新たなイベントの追加
			//System.out.println("取り消されない場合11");
			currentNode.addNewBlock(newBlock);
			
			Block addBlock = currentNode.generateNewBlock(Scheduler.getSimulationTime());
			double addBlockTime = addBlock.getTimestamp();
			Event newEvent = new FoundBlock(addBlockTime,currentNode,addBlock);
			Scheduler.addNewEvent(newEvent);
			
		}else {
			currentNode.addNewBlock(newBlock);
		}
		
	}
	
	
	
	/**
	 * 新しいブロックが見つかった場合の処理
	 * リファレンス実装の処理を想定
	 * 1.見つかったブロックの公開
	 * 2.新たなブロックの作成
	 */
	public void foundBlock(Event event) {
		Node currentNode = event.getNode();
		
		//見つかったブロックを自分のノードに追加
		FoundBlock foundBlock = (FoundBlock)event;
		Block newBlock = foundBlock.getBlock();
		
		
		super.unpublishedBlocks.add(newBlock);
		super.PublishBlock();
		
		
		//見つかったブロックの公開
		//Event newEvent = new PublishBlock(TimingRoutine.getGlobalTime(),event.getNode());
		//TimingRoutine.addNewEvent(newEvent);
	
			
		//新規ブロックの追加
		Block addBlock = currentNode.generateNewBlock(Scheduler.getSimulationTime());
		double addBlockTime = addBlock.getTimestamp();
		Event addEvent = new FoundBlock(addBlockTime,currentNode,addBlock);
		Scheduler.addNewEvent(addEvent);
		
	}
	

	
	public void initNodeScenario(Node node) {
		node.StartMining(node.generateNewBlock(Scheduler.getSimulationTime()));
	}
	
	public void initNode(Event event) {
		this.initNodeScenario(event.getNode());
	}
	

	
	public void receiveTransaction(Event event) {}
	

	
	public void sendTransaction(Event event) {}

	
	
	
}
