package jp.kota.bcasim.main.event;

import jp.kota.bcasim.configuration.NetworkChangeSpec;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.Node;

/** Changes a directed link; packets already in flight retain their scheduled delivery. */
public final class NetworkChange extends Event {
    private final Simulation simulation;
    private final NetworkChangeSpec change;
    public NetworkChange(Simulation simulation, NetworkChangeSpec change) {
        super(change.getTime(), simulation.getNetwork().getNodeList().get(change.getFrom()));
        change.validate(simulation.getConfig().getNumberOfNodes(), simulation.getConfig().getSimulationTime());
        this.simulation = simulation; this.change = change;
    }
    public int getFromIndex() { return change.getFrom(); }
    public int getToIndex() { return change.getTo(); }
    public String getAction() { return change.getAction(); }
    public void process() {
        if ("disconnect".equals(change.getAction())) simulation.getNetwork().deleteConnection(change.getFrom(), change.getTo());
        else {
            Node from = simulation.getNetwork().getNodeList().get(change.getFrom());
            Node to = simulation.getNetwork().getNodeList().get(change.getTo());
            simulation.getNetwork().addConnection(change.getFrom(), change.getTo());
            to.synchronizeWith(from);
        }
    }
}
