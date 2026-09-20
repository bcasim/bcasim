package jp.kota.bcasim.main.node;

import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.behavior.SelfishMiningBehavior;

/** Convenience constructor for the legacy node class name. */
public class Attacker4 extends Node {
    public Attacker4(Simulation simulation, String name, double weight) { super(simulation, name, weight, new SelfishMiningBehavior()); }
}
