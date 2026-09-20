package jp.kota.bcasim.main.node;

import java.util.ArrayList;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Transaction;

public final class TransactionPool {
    private final ArrayList<Transaction> transactions = new ArrayList<>();
    private final int capacity;
    public TransactionPool(SimulationConfig config) {
        capacity = (int) Math.min(Integer.MAX_VALUE, Math.floor(config.getBlockSize() / config.getTransactionSize()));
    }
    public void addNewTransaction(Transaction transaction) { transactions.add(transaction); }
    /** Snapshot selected transactions; mining does not consume pending transactions. */
    public ArrayList<Transaction> getTransactions() {
        return new ArrayList<>(transactions.subList(0, Math.min(capacity, transactions.size())));
    }
    /** Retains historical peek semantics. */
    public Transaction popTransaction() { return transactions.isEmpty() ? null : transactions.get(0); }
}
