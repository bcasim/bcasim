package jp.kota.bcasim.main.event;

import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.transaction.TransactionWorkload;

/** Arrival attempt. A send event is emitted only if the chosen sender can afford it. */
public final class GenerateTransaction extends Event {
    public GenerateTransaction(double eventTime, Node node) { super(eventTime, node); }
    @Override public void process() { TransactionWorkload.generate(node.getSimulation()); }
}
