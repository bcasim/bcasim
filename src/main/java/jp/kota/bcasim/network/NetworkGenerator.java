package jp.kota.bcasim.network;

import java.util.ArrayList;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.main.node.behavior.NodeBehaviorFactory;

public final class NetworkGenerator {
    private NetworkGenerator() {}
    public static void populate(Simulation simulation, NodeBehaviorFactory behaviors) {
        ArrayList<Node> nodes = new ArrayList<>();
        double[] weights = simulation.getConfig().getHashrates();
        String[] strategies = simulation.getConfig().getNodeStrategies();
        for (int i = 0; i < weights.length; i++)
            nodes.add(new Node(simulation, String.valueOf(i), weights[i], behaviors.create(strategies[i])));
        simulation.getNetwork().initNodeList(nodes);
    }
    public static int[][] ring(int count) {
        int[][] result = matrix(count);
        if (count > 1) for (int i = 0; i < count; i++) { result[i][(i + 1) % count] = 1; result[i][(i + count - 1) % count] = 1; }
        return result;
    }
    public static int[][] fullyConnected(int count) {
        int[][] result = matrix(count);
        for (int i = 0; i < count; i++) for (int j = 0; j < count; j++) if (i != j) result[i][j] = 1;
        return result;
    }
    public static int[][] line(int count) {
        int[][] result = matrix(count);
        for (int i = 0; i < count - 1; i++) { result[i][i + 1] = 1; result[i + 1][i] = 1; }
        return result;
    }
    private static int[][] matrix(int count) {
        if (count < 1) throw new IllegalArgumentException("Node count must be positive");
        return new int[count][count];
    }
}
