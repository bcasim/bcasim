package jp.kota.bcasim.main.event;


import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.node.Node;

public class ReceiveTransaction extends Event{
	
	
	private Transaction transaction;
	private Node from;
	
	
	public ReceiveTransaction(double eventTime,Node node,Transaction transaction) {
		super(eventTime,node);
		this.transaction = transaction;
	}
	
	
	public void process() {
		super.node.receiveTransaction(this);
	}
	
	
	public Transaction getTransaction() {
		return this.transaction;
	}
	
	public Node getFrom() {
		return this.from;
	}

}
