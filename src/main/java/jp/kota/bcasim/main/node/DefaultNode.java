package jp.kota.bcasim.main.node;

import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.behavior.HonestBehavior;

/** Convenience constructor for the legacy node class name. */
public class DefaultNode extends Node {
    public DefaultNode(Simulation simulation, String name, double weight) { super(simulation, name, weight, new HonestBehavior()); }
}
