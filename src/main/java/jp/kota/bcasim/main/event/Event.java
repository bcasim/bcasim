package jp.kota.bcasim.main.event;

import jp.kota.bcasim.main.node.Node;


public abstract class Event {
	
	
	protected int eventID;
	protected double eventTime;
	protected Node node;
	
	
	public Event(double eventTime,Node node) {
		this.eventTime = eventTime;
		this.node = node;
	}
	
	public abstract void process();
	
	
	public int getEventID() {
		return eventID;
	}
	
	
	public double getEventTime() {
		return eventTime;
	}
	
	public Node getNode() {
		return this.node;
	}
	
	public void setEventID(int eventID) {
		this.eventID = eventID;
	}
	
	public String getEventType() {
		return getClass().getSimpleName();
	}
	
	public void print() {
		System.out.println("****************:");
		System.out.println("Time: " + this.eventTime);
		System.out.println("Node: " + this.node.getName());
		System.out.println("Type: " + this.getEventType());
	}
	
}