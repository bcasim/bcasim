package jp.kota.bcasim.configuration;

import java.util.ArrayList;
import java.util.List;

/** A directed link change at a simulation timestamp. Input order breaks equal-time ties. */
public final class NetworkChangeSpec {
    private final double time;
    private final String action;
    private final int from, to;
    public NetworkChangeSpec(double time, String action, int from, int to) {
        this.time = time; this.action = action; this.from = from; this.to = to;
        if (!Double.isFinite(time) || time < 0 || (!"connect".equals(action) && !"disconnect".equals(action)))
            throw new IllegalArgumentException("Invalid network change time or action");
        if (from < 0 || to < 0 || from == to) throw new IllegalArgumentException("Invalid network change endpoints");
    }
    public void validate(int nodeCount, double duration) {
        if (from >= nodeCount || to >= nodeCount || time > duration) throw new IllegalArgumentException("Network change is outside the configured nodes or duration");
    }
    public double getTime() { return time; }
    public String getAction() { return action; }
    public int getFrom() { return from; }
    public int getTo() { return to; }
    @Override public String toString() { return time + ":" + action + ":" + from + ":" + to; }
    public static List<NetworkChangeSpec> parse(String text) {
        List<NetworkChangeSpec> changes = new ArrayList<>();
        if (text.trim().isEmpty()) return changes;
        for (String entry : text.split(";", -1)) {
            String[] fields = entry.trim().split(":", -1);
            if (fields.length != 4) throw new IllegalArgumentException("network.changes requires time:connect|disconnect:from:to entries");
            changes.add(new NetworkChangeSpec(Double.parseDouble(fields[0].trim()), fields[1].trim(),
                Integer.parseInt(fields[2].trim()), Integer.parseInt(fields[3].trim())));
        }
        return changes;
    }
}
