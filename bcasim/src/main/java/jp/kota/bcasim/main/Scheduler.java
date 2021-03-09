package jp.kota.bcasim.main;


import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.InitNode;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.network.Network;

import java.util.ArrayList;

public class Scheduler {

	
	
	private static EventList eventList = new EventList();
	private static int eventCounter = 0;
	private static double simulationTime = 0;
	
	/**
	 * 新しいイベントの追加
	 */
	public static void addNewEvent(Event event) {
		
		if(event.getEventTime() < Scheduler.simulationTime) {
			System.out.println("イベント時間エラー");
			System.exit(0);
		}
		
		event.setEventID(Scheduler.eventCounter);
		Scheduler.eventCounter = eventCounter + 1;
		Scheduler.eventList.pushEvent(event);
	}
	
	/**
	 * 次に発生するイベントの処理に着手
	 */
	public static void processEvent() {
		while(true) {
			
			//イベントリストが空の場合処理を停止する
			if(eventList.getEventList().size()==0) {
				break;
			}
			
			Event event = Scheduler.eventList.popEvent();
			
			//終了時間を超えていないかをチェックする
			if(Configuration.SIMULATION_TIME <  event.getEventTime()) {
				ArrayList<Node> nodeList = Network.getNodeList();
				for(int i=0;nodeList.size() > i;i++) {
					nodeList.get(i).PublishBlock();
				}
				break;
			}
			
			event.print();
			Scheduler.simulationTime = event.getEventTime();
			event.process();
		}
	}
	
	/**
	 * 引数で渡されたイベントIDのイベントをキューから削除
	 */
	public static void removeEvent(int eventID) {
		Scheduler.eventList.removeEvent(eventID);
	}
	
	/**
	 * これまでに処理された最後のイベント発生時刻を返す
	 */
	public static double getSimulationTime() {
		return Scheduler.simulationTime;
	}
	
	/**
	 * 指定されたノードのFoundイベントを返す
	 */
	public static FoundBlock getFoundEvent(Node node) {
		String nodeName = node.getName();
		FoundBlock event = eventList.getEventFound(nodeName);
		return event;
	}
	
	/**
	 * 初期ノードのInitイベントを追加
	 */
	public static void InitEventList() {
		ArrayList<Node > nodeList = Network.getNodeList();
		for(Node node:nodeList) {
			
			Event initEvent = new InitNode(Scheduler.simulationTime, node);
			Scheduler.addNewEvent(initEvent);
			
			//新規追加
			/*Block addBlock = node.generateNewBlock();
			double addBlockTime = addBlock.getTimestamp();
			Event addEvent = new FoundBlock(addBlockTime,node,addBlock);
			TimingRoutine.addNewEvent(addEvent);*/
		}
	}
	
	//全体のブロックを管理
	private static Blockchain blockchain = new Blockchain("main", null);
	
	public static void addBlock(Block block) {
		Scheduler.blockchain.addBlock(block);
	}
	public static Blockchain getBlockchain() {
		return Scheduler.blockchain;
	}
	
}
