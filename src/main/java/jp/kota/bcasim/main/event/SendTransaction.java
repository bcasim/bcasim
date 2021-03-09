package jp.kota.bcasim.main.event;

import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.node.Node;

public class SendTransaction extends Event{
	
	private Transaction transaction;
	
	
	public SendTransaction(double eventTime,Node node,Transaction transaction) {
		super(eventTime,node);
		this.transaction = transaction;
	}
	
	
	public void process() {
		super.node.sendTransaction(this);
	}
	
	
	public Transaction getTransaction() {
		return this.transaction;
	}

}
