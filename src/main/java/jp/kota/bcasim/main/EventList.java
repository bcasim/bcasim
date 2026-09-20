package jp.kota.bcasim.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;

/**
 * Simulation event queue, ordered by time and then registration order.
 *
 * <p>Only one mining event may be pending for each node name. Scheduling a new
 * mining event cancels its predecessor, without affecting other event types.
 * Registration freezes the event's ID, and an event instance can only be
 * registered once. Its time and node are immutable.</p>
 */
public class EventList {
    private PriorityQueue<ScheduledEvent> queue = new PriorityQueue<ScheduledEvent>();
    private final Set<ScheduledEvent> active = new HashSet<ScheduledEvent>();
    private final Map<Integer, TreeSet<ScheduledEvent>> byId =
            new HashMap<Integer, TreeSet<ScheduledEvent>>();
    private final Map<String, ScheduledEvent> miningByNode =
            new HashMap<String, ScheduledEvent>();
    private long nextSequence;

    /** Adds an event in amortized O(log n), retaining FIFO order at equal times. */
    public void pushEvent(Event event) {
        Objects.requireNonNull(event, "event");
        if (Double.isNaN(event.getEventTime())) {
            throw new IllegalArgumentException("Event time must not be NaN");
        }
        String miningNode = event instanceof FoundBlock ? event.getNode().getName() : null;
        // Reject invalid reuse before cancelling any pending mining event.
        event.markScheduled();
        if (event instanceof FoundBlock) {
            ScheduledEvent previous = miningByNode.get(miningNode);
            if (previous != null) {
                deactivate(previous);
            }
        }

        ScheduledEvent scheduled = new ScheduledEvent(event, nextSequence++, miningNode);
        queue.add(scheduled);
        active.add(scheduled);
        TreeSet<ScheduledEvent> matchingIds = byId.get(scheduled.id);
        if (matchingIds == null) {
            matchingIds = new TreeSet<ScheduledEvent>();
            byId.put(scheduled.id, matchingIds);
        }
        matchingIds.add(scheduled);
        if (event instanceof FoundBlock) {
            miningByNode.put(miningNode, scheduled);
        }
        compactIfNeeded();
    }

    /** Returns and removes the next event. Recording belongs to the scheduler. */
    public Event popEvent() {
        discardCancelledHead();
        ScheduledEvent next = queue.poll();
        if (next == null) {
            return null;
        }
        deactivate(next);
        compactIfNeeded();
        return next.event;
    }

    /** Returns the next event without consuming it, or null for an empty queue. */
    public Event peekEvent() {
        discardCancelledHead();
        ScheduledEvent next = queue.peek();
        return next == null ? null : next.event;
    }

    /**
     * Cancels the earliest matching event. Duplicate IDs are supported for
     * callers using unassigned IDs; the scheduler assigns unique IDs normally.
     */
    public void removeEvent(int eventID) {
        TreeSet<ScheduledEvent> matchingIds = byId.get(eventID);
        if (matchingIds != null) {
            deactivate(matchingIds.first());
            compactIfNeeded();
        }
    }

    /** Looks up the pending mining event for a node in O(1). */
    public FoundBlock getEventFound(String nodeName) {
        ScheduledEvent scheduled = miningByNode.get(nodeName);
        return scheduled == null ? null : (FoundBlock) scheduled.event;
    }

    public boolean isEmpty() {
        return active.isEmpty();
    }

    public int size() {
        return active.size();
    }

    /** Returns a sorted snapshot; modifying it cannot change the queue. */
    public ArrayList<Event> getEventList() {
        ArrayList<ScheduledEvent> ordered = new ArrayList<ScheduledEvent>(active);
        Collections.sort(ordered);
        ArrayList<Event> snapshot = new ArrayList<Event>(ordered.size());
        for (ScheduledEvent scheduled : ordered) {
            snapshot.add(scheduled.event);
        }
        return snapshot;
    }

    private void deactivate(ScheduledEvent scheduled) {
        scheduled.cancelled = true;
        active.remove(scheduled);
        TreeSet<ScheduledEvent> matchingIds = byId.get(scheduled.id);
        matchingIds.remove(scheduled);
        if (matchingIds.isEmpty()) {
            byId.remove(scheduled.id);
        }
        if (scheduled.event instanceof FoundBlock) {
            miningByNode.remove(scheduled.miningNode, scheduled);
        }
    }

    private void discardCancelledHead() {
        while (!queue.isEmpty() && queue.peek().cancelled) {
            queue.poll();
        }
    }

    /** Bounds retained cancelled entries during repeated mining replacement. */
    private void compactIfNeeded() {
        if (active.isEmpty()) {
            queue.clear();
        } else if (queue.size() - active.size() > Math.max(64, active.size())) {
            // The collection constructor heapifies in linear time.
            queue = new PriorityQueue<ScheduledEvent>(active);
        }
    }

    private static final class ScheduledEvent implements Comparable<ScheduledEvent> {
        private final Event event;
        private final double time;
        private final int id;
        private final long sequence;
        private final String miningNode;
        private boolean cancelled;

        private ScheduledEvent(Event event, long sequence, String miningNode) {
            this.event = event;
            // Preserve FIFO across +0.0 and -0.0, as with the original queue.
            this.time = event.getEventTime() == 0.0 ? 0.0 : event.getEventTime();
            this.id = event.getEventID();
            this.sequence = sequence;
            this.miningNode = miningNode;
        }

        @Override
        public int compareTo(ScheduledEvent other) {
            int timeOrder = Double.compare(time, other.time);
            return timeOrder != 0 ? timeOrder : Long.compare(sequence, other.sequence);
        }
    }
}
