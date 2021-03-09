package jp.kota.bcasim.network;


import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.main.node.Node;

import java.util.ArrayList;

public class Network {
	
	
	/**
	 * ノードの隣接行列を保持する
	 */
	private static int[][] adjacencyMatrix;
	
	
	/**
	 * ネットワークの参加ノードを管理するリスト
	 */
	private static ArrayList<Node> nodeList = new ArrayList<Node>();
	
	
	/**
	 * 初期ネットワーク情報を設定
	 * 隣接行列を設定
	 */
	public static void initAdjacencyMatrix(int[][] adjacencyMatrix) {
		Network.adjacencyMatrix = adjacencyMatrix;
	}
	
	/**
	 * 初期ノードリストを設定
	 */
	public static void initNodeList(ArrayList<Node> nodeList) {
		Network.nodeList = nodeList;
	}
	
	
	/**
	 * 引数で指定されたノードの隣接ノードを返す
	 */
	public static ArrayList<Node> getAdjacencyNode(String name) {
		ArrayList<Node> adjacencyNode = new ArrayList<Node>();
		int nodeIndex = -1;
		for(int i=0;i<Network.nodeList.size();i++) {
			String nodeName = Network.nodeList.get(i).getName();
			if(nodeName.equals(name)) {
				nodeIndex = i;
				break;
			}
		}
		for(int i=0;i<Network.adjacencyMatrix[nodeIndex].length;i++) {
			if(Network.adjacencyMatrix[nodeIndex][i]==1) {
				adjacencyNode.add(nodeList.get(i));
			}
		}
		
		return adjacencyNode;
	}
	
	/**
	 * 新規ノードをネットワークに追加
	 */
	public static void addNewNode(Node node) {
		int _adjacencyMatrix[][] = new int[Network.adjacencyMatrix.length+1][Network.adjacencyMatrix.length+1];
		
		for(int i=0;i<Network.adjacencyMatrix.length;i++) {
			for (int j = 0; j < Network.adjacencyMatrix.length; j++) {
				_adjacencyMatrix[i][j] = adjacencyMatrix[i][j];
			}
		}
		Network.adjacencyMatrix = _adjacencyMatrix;
		Network.nodeList.add(node);
	}
	
	
	/**
	 * ノード間の接続を追加
	 */
	public static void addConnection(int from,int to) {
		Network.adjacencyMatrix[from][to] = 1;
	}
	
	/**
	 * ノード間の接続を削除
	 */
	public static void deleteConnection(int from,int to) {
		Network.adjacencyMatrix[from][to] = 0;
	}
	
	
	/**
	 * ノード間のブロック伝送遅延を返す
	 */
	public static double getBlockDelay() {
		return Configuration.BLOCK_DELAY;
	}
	
	/**
	 * ノード間のブロック伝送遅延を返す
	 */
	public static double getTransactionDelay() {
		return Configuration.TRANSACTION_DELAY;
	}
	
	
	/**
	 * ノードの隣接行列を返す
	 */
	public static int[][] getAdjacencyMatrix() {
		return Network.adjacencyMatrix;
	}
	
	/**
	 * ノードリストを返す
	 */
	public static ArrayList<Node> getNodeList(){
		return Network.nodeList;
	}
	
	
	
	
	/**
	 * 隣接行列を出力
	 */
	public static void printNetwork() {
		for(int i=0;i<Network.adjacencyMatrix.length;i++) {
			for(int j=0;j<Network.adjacencyMatrix.length;j++) {
				System.out.print(Network.adjacencyMatrix[i][j] + " ");
			}
			System.out.println();
		}
	}
	/**
	 * ノードリストを出力
	 */
	public static void printNodeList() {
		for(int i=0;i<Network.nodeList.size();i++) {
			System.out.println("id:"+Network.nodeList.get(i).getName()+"|"+ "hashrate:"+Network.nodeList.get(i).getHashrate());
		}
	}
	
	
	
	
}
