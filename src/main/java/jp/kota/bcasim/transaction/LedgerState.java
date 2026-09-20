package jp.kota.bcasim.transaction;

import java.util.Arrays;
import java.util.List;
import jp.kota.bcasim.datastructure.Transaction;

/** Immutable account model. Node IDs are account IDs; no signatures, fees or UTXOs are modeled. */
public final class LedgerState {
    private final double[] balances;
    private final long[] nonces;

    private LedgerState(double[] balances, long[] nonces) {
        this.balances = balances; this.nonces = nonces;
    }
    public static LedgerState initial(int accounts, double balance) {
        if (accounts < 0 || !Double.isFinite(balance) || balance < 0) throw new IllegalArgumentException("Invalid initial ledger");
        double[] balances = new double[accounts];
        Arrays.fill(balances, balance);
        return new LedgerState(balances, new long[accounts]);
    }
    private int account(String name) {
        try {
            int index = Integer.parseInt(name);
            if (index >= 0 && index < balances.length && String.valueOf(index).equals(name)) return index;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Unknown account: " + name);
    }
    public double getBalance(String account) { return balances[account(account)]; }
    public long getNextNonce(String account) { return nonces[account(account)]; }
    public double[] getBalances() { return balances.clone(); }
    public long[] getNonces() { return nonces.clone(); }
    public boolean canApply(Transaction transaction) {
        try { apply(transaction); return true; }
        catch (IllegalArgumentException invalid) { return false; }
    }
    public LedgerState apply(Transaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("Missing transaction");
        int from = account(transaction.getFrom()), to = account(transaction.getTo());
        if (transaction.getNonce() != nonces[from]) throw new IllegalArgumentException("Conflicting or out-of-order transaction nonce");
        if (transaction.getValue() > balances[from]) throw new IllegalArgumentException("Insufficient account balance");
        if (nonces[from] == Long.MAX_VALUE) throw new IllegalArgumentException("Account nonce exhausted");
        double[] nextBalances = balances.clone();
        long[] nextNonces = nonces.clone();
        nextBalances[from] -= transaction.getValue();
        nextBalances[to] += transaction.getValue();
        if (!Double.isFinite(nextBalances[to])) throw new IllegalArgumentException("Account balance overflow");
        nextNonces[from]++;
        return new LedgerState(nextBalances, nextNonces);
    }
    public LedgerState applyBlock(List<Transaction> transactions, String miner, double reward) {
        if (transactions == null || !Double.isFinite(reward) || reward < 0) throw new IllegalArgumentException("Invalid block ledger input");
        LedgerState state = this;
        java.util.Set<String> hashes = new java.util.HashSet<>();
        for (Transaction transaction : transactions) {
            if (transaction == null || !hashes.add(transaction.getTransactionHash()))
                throw new IllegalArgumentException("Duplicate transaction in block");
            state = state.apply(transaction);
        }
        int minerIndex = state.account(miner);
        double[] nextBalances = state.balances.clone();
        nextBalances[minerIndex] += reward;
        if (!Double.isFinite(nextBalances[minerIndex])) throw new IllegalArgumentException("Account balance overflow");
        return new LedgerState(nextBalances, state.nonces.clone());
    }
}
