package jp.kota.bcasim.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.main.node.Node;

/** Directed topology owned by a single experiment. */
public final class Network {
    private final SimulationConfig config;
    private int[][] adjacencyMatrix;
    private final ArrayList<Node> nodes = new ArrayList<>();
    private final Map<String, Integer> indices = new LinkedHashMap<>();
    public Network(SimulationConfig config) { this.config = config; adjacencyMatrix = config.getAdjacencyMatrix(); }
    public void initNodeList(ArrayList<Node> values) {
        if (!nodes.isEmpty()) throw new IllegalStateException("Network is already initialized");
        if (values.size() != adjacencyMatrix.length) throw new IllegalArgumentException("Node count differs from matrix size");
        for (Node node : values) {
            if (indices.put(node.getName(), nodes.size()) != null) throw new IllegalArgumentException("Duplicate node ID");
            nodes.add(node);
        }
    }
    public ArrayList<Node> getAdjacencyNode(String name) {
        Integer index = indices.get(name);
        if (index == null) throw new IllegalArgumentException("Unknown node: " + name);
        ArrayList<Node> neighbors = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) if (adjacencyMatrix[index][i] == 1) neighbors.add(nodes.get(i));
        return neighbors;
    }
    public void addNewNode(Node node) {
        if (indices.containsKey(node.getName())) throw new IllegalArgumentException("Duplicate node ID");
        if (!nodes.isEmpty() && nodes.get(0).getSimulation() != node.getSimulation()) throw new IllegalArgumentException("Node belongs to another simulation");
        int[][] expanded = new int[nodes.size() + 1][nodes.size() + 1];
        for (int i = 0; i < nodes.size(); i++) System.arraycopy(adjacencyMatrix[i], 0, expanded[i], 0, nodes.size());
        adjacencyMatrix = expanded;
        indices.put(node.getName(), nodes.size()); nodes.add(node);
    }
    public void addConnection(int from, int to) {
        check(from, to); if (from == to) throw new IllegalArgumentException("Self connection"); adjacencyMatrix[from][to] = 1;
    }
    public void deleteConnection(int from, int to) { check(from, to); adjacencyMatrix[from][to] = 0; }
    private void check(int from, int to) {
        if (from < 0 || to < 0 || from >= nodes.size() || to >= nodes.size()) throw new IllegalArgumentException("Unknown node index");
    }
    public boolean isConnected(Node from, Node to) {
        Integer a = indices.get(from.getName()), b = indices.get(to.getName());
        return a != null && b != null && nodes.get(a) == from && nodes.get(b) == to && adjacencyMatrix[a][b] == 1;
    }
    public double getBlockDelay() { return config.getBlockDelay(); }
    public double getTransactionDelay() { return config.getTransactionDelay(); }
    public int[][] getAdjacencyMatrix() {
        int[][] result = new int[adjacencyMatrix.length][];
        for (int i = 0; i < result.length; i++) result[i] = adjacencyMatrix[i].clone();
        return result;
    }
    public ArrayList<Node> getNodeList() { return new ArrayList<>(nodes); }
}
