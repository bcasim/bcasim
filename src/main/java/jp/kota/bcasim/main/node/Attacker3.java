package jp.kota.bcasim.main.node;

import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.behavior.DoubleSpendingBehavior;

/** Convenience constructor for the legacy node class name. */
public class Attacker3 extends Node {
    public Attacker3(Simulation simulation, String name, double weight) { super(simulation, name, weight, new DoubleSpendingBehavior()); }

    public void print_rate() {
        DoubleSpendingBehavior b = (DoubleSpendingBehavior) getBehavior();
        int total = b.getSuccesses() + b.getFailures();
        System.out.println("fail:" + b.getFailures() + " succ:" + b.getSuccesses() + " rate:" + (total == 0 ? 0 : (double)b.getSuccesses()/total));
    }
}
