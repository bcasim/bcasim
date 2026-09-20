package jp.kota.bcasim.main.event;

import jp.kota.bcasim.main.node.Node;


public abstract class Event {
	
	
	private int eventID;
	private boolean scheduled;
	protected final double eventTime;
	protected final Node node;
	
	
	public Event(double eventTime,Node node) {
		this.eventTime = eventTime;
		this.node = node;
	}
	
	public abstract void process();
	
	
	public final int getEventID() {
		return eventID;
	}
	
	
	public final double getEventTime() {
		return eventTime;
	}
	
	public final Node getNode() {
		return this.node;
	}
	
	/** Sets the scheduler-assigned ID before the event's first registration. */
	public final void setEventID(int eventID) {
		if (scheduled) {
			throw new IllegalStateException("A registered event's ID cannot change");
		}
		this.eventID = eventID;
	}

	/**
	 * Called by EventList on first registration to freeze this event's identity.
	 * Event instances are single-use: create a new event to schedule another
	 * occurrence, even after this event has been processed or cancelled.
	 *
	 * @throws IllegalStateException if this instance has already been registered
	 */
	public final void markScheduled() {
		if (scheduled) {
			throw new IllegalStateException("An event instance can only be scheduled once");
		}
		scheduled = true;
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
