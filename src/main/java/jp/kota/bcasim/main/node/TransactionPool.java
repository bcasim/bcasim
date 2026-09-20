package jp.kota.bcasim.main.node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.transaction.LedgerState;

/** Insertion-ordered pending transactions, shared by public and private mining parents. */
public final class TransactionPool {
    private final Map<String, Transaction> transactions = new LinkedHashMap<>();
    private final int capacity;
    public TransactionPool(SimulationConfig config) {
        capacity = (int) Math.min(Integer.MAX_VALUE, Math.floor(config.getBlockSize() / config.getTransactionSize()));
    }
    public void addNewTransaction(Transaction transaction) { transactions.putIfAbsent(transaction.getTransactionHash(), transaction); }
    public boolean accept(Transaction transaction, Block tip) {
        if (transaction == null || transactions.containsKey(transaction.getTransactionHash())) return false;
        LedgerState state = tip.getLedgerState();
        try {
            state.getBalance(transaction.getTo());
            if (transaction.getNonce() < state.getNextNonce(transaction.getFrom())) return false;
            if (transaction.getValue() > state.getBalance(transaction.getFrom()) &&
                transaction.getValue() > projectedState(tip).getBalance(transaction.getFrom())) return false;
        } catch (IllegalArgumentException invalid) { return false; }
        addNewTransaction(transaction);
        return true;
    }
    /** Compatibility snapshot without parent validation. */
    public ArrayList<Transaction> getTransactions() {
        ArrayList<Transaction> result = new ArrayList<>(transactions.values());
        return new ArrayList<>(result.subList(0, Math.min(capacity, result.size())));
    }
    /** Select a nonce-ordered, affordable payload without consuming pending transactions. */
    public ArrayList<Transaction> getTransactions(Block parent) { return select(parent, capacity); }
    private ArrayList<Transaction> select(Block parent, int limit) {
        ArrayList<Transaction> selected = new ArrayList<>();
        LedgerState state = parent.getLedgerState();
        java.util.Set<String> chosen = new java.util.HashSet<>();
        boolean progress = true;
        while (progress && selected.size() < limit) {
            progress = false;
            for (Transaction transaction : transactions.values()) {
                if (selected.size() == limit) break;
                if (!chosen.contains(transaction.getTransactionHash()) && state.canApply(transaction)) {
                    state = state.apply(transaction);
                    selected.add(transaction); chosen.add(transaction.getTransactionHash()); progress = true;
                }
            }
        }
        return selected;
    }
    public LedgerState projectedState(Block parent) {
        LedgerState state = parent.getLedgerState();
        for (Transaction transaction : select(parent, Integer.MAX_VALUE)) state = state.apply(transaction);
        return state;
    }
    /** Remove newly confirmed transactions, then return detached transactions after a reorganization. */
    public void reconcile(Block oldTip, Block newTip) {
        if (oldTip.getHash().equals(newTip.getHash())) return;
        ArrayList<Block> detached = new ArrayList<>(), attached = new ArrayList<>();
        Block old = oldTip, next = newTip;
        while (old.getHeight() > next.getHeight()) { detached.add(old); old = old.getPreviousBlock(); }
        while (next.getHeight() > old.getHeight()) { attached.add(next); next = next.getPreviousBlock(); }
        while (!old.getHash().equals(next.getHash())) {
            detached.add(old); attached.add(next); old = old.getPreviousBlock(); next = next.getPreviousBlock();
        }
        for (Block block : attached) for (Transaction transaction : block.getTransactionList()) transactions.remove(transaction.getTransactionHash());
        // Keep detached transactions even when their funding transaction is also pending.
        // Selection revalidates them against the new branch, including nonce conflicts.
        for (int i = detached.size() - 1; i >= 0; i--) for (Transaction transaction : detached.get(i).getTransactionList()) addNewTransaction(transaction);
        LedgerState state = newTip.getLedgerState();
        transactions.values().removeIf(transaction -> {
            try { return transaction.getNonce() < state.getNextNonce(transaction.getFrom()); }
            catch (IllegalArgumentException unknownAccount) { return true; }
        });
    }
    public int size() { return transactions.size(); }
    public boolean contains(String hash) { return transactions.containsKey(hash); }
    /** Retains historical peek semantics. */
    public Transaction popTransaction() { return transactions.isEmpty() ? null : transactions.values().iterator().next(); }
}
