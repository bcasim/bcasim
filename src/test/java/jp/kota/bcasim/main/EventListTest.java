package jp.kota.bcasim.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.node.DefaultNode;
import jp.kota.bcasim.main.node.Node;

public class EventListTest {
    @Test
    public void ordersByTimeAndPreservesRegistrationOrderAtTies() {
        EventList queue = new EventList();
        Event later = event(10.0, 1);
        Event first = event(2.0, 2);
        Event second = event(2.0, 3);
        queue.pushEvent(later);
        queue.pushEvent(first);
        queue.pushEvent(second);

        assertSame(first, queue.peekEvent());
        assertEquals(3, queue.size());
        assertSame(first, queue.popEvent());
        assertSame(second, queue.popEvent());
        assertSame(later, queue.popEvent());
        assertTrue(queue.isEmpty());
        assertNull(queue.peekEvent());
        assertNull(queue.popEvent());
    }

    @Test
    public void positiveAndNegativeZeroShareRegistrationOrder() {
        EventList queue = new EventList();
        Event first = event(0.0, 1);
        Event second = event(-0.0, 2);
        queue.pushEvent(first);
        queue.pushEvent(second);
        assertSame(first, queue.popEvent());
        assertSame(second, queue.popEvent());
    }

    @Test
    public void cancellingAnIdLeavesOtherEventsAndAllowsIdReuse() {
        EventList queue = new EventList();
        Event cancelled = event(1.0, 1);
        Event retained = event(2.0, 2);
        Event replacement = event(3.0, 1);
        queue.pushEvent(cancelled);
        queue.pushEvent(retained);
        queue.removeEvent(1);
        queue.removeEvent(999);
        queue.pushEvent(replacement);

        assertEquals(2, queue.size());
        assertSame(retained, queue.popEvent());
        assertSame(replacement, queue.popEvent());
        assertNull(queue.popEvent());
    }

    @Test
    public void duplicateIdsCancelTheEarliestMatchingEvent() {
        EventList queue = new EventList();
        Event later = event(3.0, 0);
        Event earliest = event(1.0, 0);
        Event sameTime = event(1.0, 0);
        queue.pushEvent(later);
        queue.pushEvent(earliest);
        queue.pushEvent(sameTime);
        queue.removeEvent(0);
        assertSame(sameTime, queue.popEvent());
        assertSame(later, queue.popEvent());
    }

    @Test
    public void snapshotIsSortedAndCannotModifyTheQueue() {
        EventList queue = new EventList();
        Event first = event(1.0, 1);
        Event last = event(3.0, 2);
        queue.pushEvent(last);
        queue.pushEvent(first);
        ArrayList<Event> snapshot = queue.getEventList();
        assertSame(first, snapshot.get(0));
        assertSame(last, snapshot.get(1));
        snapshot.clear();
        assertFalse(queue.isEmpty());
        assertEquals(2, queue.size());
        assertSame(first, queue.popEvent());
    }

    @Test
    public void registeredEventIdsCannotBeChanged() {
        EventList queue = new EventList();
        Event pending = event(1.0, 7);
        queue.pushEvent(pending);
        expectIllegalState(() -> pending.setEventID(8));
        assertEquals(7, pending.getEventID());
        assertSame(pending, queue.peekEvent());
        queue.removeEvent(7);
        assertTrue(queue.isEmpty());
        expectIllegalState(() -> pending.setEventID(9));
    }

    @Test
    public void duplicateRegistrationLeavesThePendingEventIntact() {
        EventList queue = new EventList();
        Event pending = event(1.0, 7);
        queue.pushEvent(pending);
        expectIllegalState(() -> queue.pushEvent(pending));
        assertEquals(1, queue.size());
        assertSame(pending, queue.peekEvent());
        assertSame(pending, queue.popEvent());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void processedEventsCannotBeRegisteredAgainEvenInAnotherQueue() {
        EventList first = new EventList();
        EventList second = new EventList();
        Event processed = event(1.0, 7);
        first.pushEvent(processed);
        assertSame(processed, first.popEvent());
        expectIllegalState(() -> first.pushEvent(processed));
        expectIllegalState(() -> second.pushEvent(processed));
        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
    }

    @Test
    public void resubmittingCancelledMiningCannotCancelItsReplacement() {
        Simulation simulation = new Simulation(SimulationConfig.defaults());
        Node node = new DefaultNode(simulation, "0", 0.5);
        EventList queue = new EventList();
        FoundBlock original = mining(1.0, 1, node);
        FoundBlock replacement = mining(2.0, 2, node);
        queue.pushEvent(original);
        queue.pushEvent(replacement);
        expectIllegalState(() -> queue.pushEvent(original));
        assertEquals(1, queue.size());
        assertSame(replacement, queue.getEventFound("0"));
        assertSame(replacement, queue.popEvent());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void miningReplacementUsesNodeNameAndKeepsOtherEventTypes() {
        Simulation simulation = new Simulation(SimulationConfig.defaults());
        Node node = new DefaultNode(simulation, "0", 0.5);
        Node sameName = new DefaultNode(simulation, "0", 0.5);
        Node otherNode = new DefaultNode(simulation, "1", 0.5);
        EventList queue = new EventList();
        FoundBlock oldMining = mining(1.0, 1, node);
        FoundBlock replacement = mining(4.0, 2, sameName);
        FoundBlock otherMining = mining(3.0, 3, otherNode);
        Event nonMining = new Event(2.0, node) {
            @Override public void process() { }
        };
        nonMining.setEventID(4);
        queue.pushEvent(oldMining);
        queue.pushEvent(nonMining);
        queue.pushEvent(otherMining);
        queue.pushEvent(replacement);

        assertEquals(3, queue.size());
        assertSame(replacement, queue.getEventFound("0"));
        assertSame(otherMining, queue.getEventFound("1"));
        assertNull(queue.getEventFound("missing"));
        assertSame(nonMining, queue.popEvent());
        queue.removeEvent(2);
        assertNull(queue.getEventFound("0"));
        assertSame(otherMining, queue.popEvent());
        assertNull(queue.getEventFound("1"));
        assertTrue(queue.isEmpty());
    }

    @Test
    public void repeatedMiningReplacementNeverReturnsCancelledEvents() {
        Simulation simulation = new Simulation(SimulationConfig.defaults());
        Node node = new DefaultNode(simulation, "0", 0.5);
        EventList queue = new EventList();
        Event earliest = event(0.0, -1);
        queue.pushEvent(earliest);
        FoundBlock latest = null;
        for (int i = 1; i <= 10000; i++) {
            latest = mining(i, i, node);
            queue.pushEvent(latest);
        }

        assertEquals(2, queue.size());
        assertSame(latest, queue.getEventFound("0"));
        assertSame(earliest, queue.popEvent());
        assertSame(latest, queue.popEvent());
        assertNull(queue.popEvent());
    }

    @Test
    public void mixedOperationsMatchAStableListReference() {
        EventList queue = new EventList();
        ArrayList<Event> reference = new ArrayList<Event>();
        Simulation simulation = new Simulation(SimulationConfig.defaults());
        Node[] nodes = {
            new DefaultNode(simulation, "0", 0.5),
            new DefaultNode(simulation, "1", 0.5)
        };
        Random random = new Random(74231L);
        for (int step = 0; step < 5000; step++) {
            int operation = random.nextInt(4);
            if (operation < 2) {
                double time = random.nextInt(20);
                int id = random.nextInt(15);
                Event added = random.nextInt(3) == 0
                        ? mining(time, id, nodes[random.nextInt(nodes.length)]) : event(time, id);
                queue.pushEvent(added);
                if (added instanceof FoundBlock) {
                    for (int i = 0; i < reference.size(); i++) {
                        Event pending = reference.get(i);
                        if (pending instanceof FoundBlock
                                && pending.getNode().getName().equals(added.getNode().getName())) {
                            reference.remove(i);
                            break;
                        }
                    }
                }
                int position = 0;
                while (position < reference.size()
                        && reference.get(position).getEventTime() <= added.getEventTime()) {
                    position++;
                }
                reference.add(position, added);
            } else if (operation == 2) {
                int id = random.nextInt(15);
                queue.removeEvent(id);
                for (int i = 0; i < reference.size(); i++) {
                    if (reference.get(i).getEventID() == id) {
                        reference.remove(i);
                        break;
                    }
                }
            } else {
                assertSame(reference.isEmpty() ? null : reference.remove(0), queue.popEvent());
            }
            assertEquals(reference.size(), queue.size());
            assertSame(reference.isEmpty() ? null : reference.get(0), queue.peekEvent());
            for (Node node : nodes) {
                Event expectedMining = null;
                for (Event pending : reference) {
                    if (pending instanceof FoundBlock
                            && pending.getNode().getName().equals(node.getName())) {
                        expectedMining = pending;
                        break;
                    }
                }
                assertSame(expectedMining, queue.getEventFound(node.getName()));
            }
        }
        assertEquals(reference, queue.getEventList());
        for (Event expected : reference) {
            assertSame(expected, queue.popEvent());
        }
        assertTrue(queue.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNaNWhichHasNoSchedulingOrder() {
        new EventList().pushEvent(event(Double.NaN, 1));
    }

    private static Event event(double time, int id) {
        Event result = new Event(time, null) {
            @Override public void process() { }
        };
        result.setEventID(id);
        return result;
    }

    private static void expectIllegalState(Runnable operation) {
        try {
            operation.run();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // The failed operation must preserve the caller's queue state.
        }
    }

    private static FoundBlock mining(double time, int id, Node node) {
        FoundBlock result = new FoundBlock(time, node, null);
        result.setEventID(id);
        return result;
    }
}
