package jp.kota.bcasim.datastructure;


import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.HashGenerator;

import java.util.HashSet;
import java.util.Set;


public class Transaction {
	
	
	private String from;
	private String to;
	private int value;
	private String transactionHash;
	private int id;
	
	private Set<Node> transmittedNodes = new HashSet<>();
	
	public Transaction(
			String from,
			String to,
			int value,
			String transactionHash) {
		
		this.from = from;
		this.to = to;
		this.value = value;
		this.transactionHash = HashGenerator
				.generateHash(from + to + value + String.valueOf(Math.random()));
		
	}
	
	public String getFrom() {
		return this.from;
	}
	
	public String getTo() {
		return this.to;
	}
	
	public int getValue() {
		return this.value;
	}
	
	public String getTransactionHash() {
		return this.transactionHash;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void print() {
		System.out.println("{");
		System.out.println("  ”from” : " + this.from + ",");
		System.out.println("  ”to” : " + this.to + ",");
		System.out.println("  ”value” : " + this.value + ",");
		System.out.println("  ”value” : " + this.transactionHash + ",");
		System.out.println("}");
	}
	
	/*
	 * トランザクションが伝送済みノードを記録
	 */
	public Set<Node> getTransmittedNodes(){
		return this.transmittedNodes;
	}
	
	public void addTransmittedNodes(Node node) {
		this.transmittedNodes.add(node);
	}
	
	public void setTransmittedNodes(Set<Node> transmittedNodes) {
		this.transmittedNodes = transmittedNodes;
	}
	
	public boolean verifyNode(Node node) {
		return this.transmittedNodes.contains(node);
	}
	
	
}
