package jp.kota.bcasim.main.node;


import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.datastructure.Transaction;

import java.util.ArrayList;



public class TransactionPool {
	
	
	
	ArrayList<Transaction> transactionList;
	
	
	
	public TransactionPool() {
		this.transactionList = new ArrayList<Transaction>();
	}
	
	
	
	public void addNewTransaction(Transaction transaction) {
		this.transactionList.add(transaction);
	}
	
	
	public ArrayList<Transaction> getTransactions() {
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();
		if(transactionList.size()!=0) {
			return transactions;
		}
		
		for(int i=0;i<transactionList.size();i++) {
			if(Configuration.BLOCK_SIZE/Configuration.TRANSACTION_SIZE<i) {
				break;
			}
			transactions.add(transactionList.get(i));
		}
		return transactions;
	}
	
	public Transaction popTransaction() {
		if(transactionList.size()!=0) {
			Transaction tx = transactionList.get(0);
			//transactionList.remove(0);
			return tx;
		}else {
			return null;
		}
	}

}
