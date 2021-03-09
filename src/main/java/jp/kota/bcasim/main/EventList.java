package jp.kota.bcasim.main;

import java.util.ArrayList;

import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.tool.fileio.OutputResult;

public class EventList {
	
	
	/*
	 * 同じノードのFoundBlockイベントはキューの中に
	 * 一つだけに制限される必要がある
	 */
	private ArrayList<Event> eventList;
	
	public EventList() {
		this.eventList = new ArrayList<Event>();
	}
	
	/**
	 * 新しいイベントを追加
	 * 発生時刻が速い順になるようにイベントを追加
	 */
	/*
	//高速化したがバグが混入
	public void pushEvent(Event newEvent) {
		double newEventTime = newEvent.getEventTime();
		for(int i = this.eventQueue.size()-1; i >= 0; i--) {
			double eventTime = this.eventQueue.get(i).getEventTime();
			if(newEventTime >= eventTime) {
				this.eventQueue.add(i+1,newEvent);
				return;
			}
		}
		this.eventQueue.add(newEvent);
	}*/
	public void pushEvent(Event newEvent) {
		double newEventTime = newEvent.getEventTime();
		
		
		//Foundイベントの重複を確認
		if(newEvent.getEventType().equals("FoundBlock")) {
			
			for(int i=0;i < this.eventList.size();i++) {
				if(this.eventList.get(i).getNode().getName().equals(newEvent.getNode().getName())
						&&this.eventList.get(i).getEventType().equals("FoundBlock")) 
				{
					this.eventList.remove(i);
					break;
				}
			}
		}
		
		for(int i = 0; i < this.eventList.size(); i++) {
			Event event =  this.eventList.get(i);
			double eventTime = event.getEventTime();
			
			if(newEventTime < eventTime) {
				this.eventList.add(i,newEvent);
				return;
			}
		}
		this.eventList.add(newEvent);
	}
	
	
	
	
	/**
	 * 発生時刻の速いイベントを返す
	 */
	public Event popEvent() {
		
		if (this.eventList.size() == 0) {
			return null;
		}
		Event event = this.eventList.get(0);
		
		OutputResult.outEventJson(event);
		
		this.eventList.remove(0);
		return event;
	}
	
	
	
	/**
	 * 指定されたイベントIDのイベントを削除
	 */
	public void removeEvent(int eventID) {
		for(int i = 0; i < this.eventList.size(); i++) {
			Event event = this.eventList.get(i);
			int _eventID =  event.getEventID();
			if(eventID == _eventID) {
				System.out.println("イベント削除");
				this.eventList.remove(i);
				break;
			}
		}
	}
	
	/**
	 * 指定されたノードのFoundイベントを返す
	 */
	public FoundBlock getEventFound(String nodeName) {
		
		for(Event event : eventList) {
			String _nodeName = event.getNode().getName();
			if(!event.getEventType().equals("FoundBlock")) {
				continue;
			}
			
			if(nodeName.equals(_nodeName)) {
				return (FoundBlock)event;
			}
		}
		return null;
	}
	
	public ArrayList<Event> getEventList(){
		return this.eventList;
	}

	

}
