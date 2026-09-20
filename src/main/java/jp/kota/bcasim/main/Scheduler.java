package jp.kota.bcasim.main;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.InitNode;
import jp.kota.bcasim.main.node.Node;

public final class Scheduler {
    private final Simulation simulation;
    private final EventList eventList = new EventList();
    private int eventCounter;
    private long processedEvents;
    private double simulationTime;
    Scheduler(Simulation simulation) { this.simulation = simulation; }
    public void addNewEvent(Event event) {
        if (!Double.isFinite(event.getEventTime()) || event.getEventTime() < simulationTime)
            throw new IllegalArgumentException("Event time must be finite and at or after the simulation clock");
        if (event.getNode().getSimulation() != simulation) throw new IllegalArgumentException("Event belongs to another simulation");
        if (eventCounter == Integer.MAX_VALUE) throw new IllegalStateException("Event ID limit exceeded");
        event.setEventID(eventCounter);
        eventList.pushEvent(event);
        eventCounter++;
    }
    public void processEvent() {
        while (!eventList.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Simulation interrupted");
            // Check before dequeue/record: the trace only contains executed events.
            if (eventList.peekEvent().getEventTime() > simulation.getConfig().getSimulationTime()) {
                // Retain historical final publication, at the last processed time.
                for (Node node : simulation.getNetwork().getNodeList()) node.PublishBlock();
                break;
            }
            Event event = eventList.popEvent();
            simulationTime = event.getEventTime();
            simulation.getWriter().recordEvent(event);
            event.process();
            processedEvents++;
        }
    }
    public void InitEventList() {
        for (Node node : simulation.getNetwork().getNodeList()) addNewEvent(new InitNode(simulationTime, node));
    }
    public void removeEvent(int id) { eventList.removeEvent(id); }
    public FoundBlock getFoundEvent(Node node) { return eventList.getEventFound(node.getName()); }
    public double getSimulationTime() { return simulationTime; }
    public long getProcessedEventCount() { return processedEvents; }
    public EventList getEventList() { return eventList; }
    public void addBlock(Block block) { simulation.getBlockchain().addBlock(block); }
    public Blockchain getBlockchain() { return simulation.getBlockchain(); }
}
