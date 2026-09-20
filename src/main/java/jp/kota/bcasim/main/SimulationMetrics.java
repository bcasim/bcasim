package jp.kota.bcasim.main;

import java.util.*;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.main.node.behavior.DoubleSpendingBehavior;

/** Snapshot metrics with an explicit observer chain and denominators. */
public final class SimulationMetrics {
    private SimulationMetrics() {}
    public static Map<String, Object> collect(Simulation simulation) {
        List<Node> nodes = simulation.getNetwork().getNodeList();
        int observer = simulation.getConfig().getObserverNode();
        Blockchain chain = nodes.get(observer).getBlockchain();
        int accepted = 0, attackerAccepted = 0;
        Set<String> transactions = new HashSet<>();
        Map<String, Integer> byMiner = new LinkedHashMap<>();
        for (Node node : nodes) byMiner.put(node.getName(), 0);
        for (Block block = chain.getLatestBlock(); block != null && block.getHeight() > 0; block = block.getPreviousBlock()) {
            accepted++;
            String miner = block.getMiner().getName();
            byMiner.put(miner, byMiner.getOrDefault(miner, 0) + 1);
            if (miner.equals(nodes.get(0).getName())) attackerAccepted++;
            for (Transaction transaction : block.getTransactionList()) transactions.add(transaction.getTransactionHash());
        }
        int published = walk(simulation.getBlockchain()).size() - 1;
        long successes = 0, failures = 0, receipts = 0;
        double delaySum = 0;
        for (Node node : nodes) {
            if (node.getBehavior() instanceof DoubleSpendingBehavior) {
                DoubleSpendingBehavior behavior = (DoubleSpendingBehavior) node.getBehavior();
                successes += behavior.getSuccesses(); failures += behavior.getFailures();
            }
            for (Block block : walk(node.getBlockchain())) {
                if (block.getHeight() == 0 || block.getMiner() == node) continue;
                Block first = simulation.getBlockchain().serchBlockHash(block.getHash());
                if (first != null) { delaySum += block.getReceiveBlockTime() - first.getReceiveBlockTime(); receipts++; }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("observerNode", observer);
        result.put("acceptedBlocks", accepted);
        result.put("totalPublishedBlocks", published);
        result.put("staleBlocks", published - accepted);
        result.put("staleFraction", published == 0 ? null : (double)(published - accepted) / published);
        result.put("attackerNode", 0);
        result.put("attackerAcceptedBlocks", attackerAccepted);
        result.put("attackerRevenueShare", accepted == 0 ? null : (double)attackerAccepted / accepted);
        result.put("mainchainByMiner", Collections.unmodifiableMap(byMiner));
        result.put("forkPoints", chain.getForkPoints());
        result.put("reorgCount", chain.getReorgCount());
        result.put("maxReorgDepth", chain.getMaxReorgDepth());
        result.put("remoteBlockReceipts", receipts);
        result.put("meanPropagationDelay", receipts == 0 ? null : delaySum / receipts);
        result.put("attackSuccesses", successes);
        result.put("attackFailures", failures);
        result.put("attackTrialsCompleted", successes + failures);
        result.put("attackSuccessRate", successes + failures == 0 ? null : (double)successes / (successes + failures));
        result.put("transactionsConfirmed", transactions.size());
        Map<String, Double> balances = new LinkedHashMap<>();
        Map<String, Long> nonces = new LinkedHashMap<>();
        Map<String, Integer> pending = new LinkedHashMap<>(), orphans = new LinkedHashMap<>();
        Map<String, Long> rejected = new LinkedHashMap<>();
        for (Node node : nodes) {
            balances.put(node.getName(), chain.getLatestBlock().getLedgerState().getBalance(node.getName()));
            nonces.put(node.getName(), chain.getLatestBlock().getLedgerState().getNextNonce(node.getName()));
            pending.put(node.getName(), node.getTransactionPool().size());
            orphans.put(node.getName(), node.getOrphanBlockCount());
            rejected.put(node.getName(), node.getRejectedBlockCount());
        }
        result.put("canonicalBalances", Collections.unmodifiableMap(balances));
        result.put("canonicalNonces", Collections.unmodifiableMap(nonces));
        result.put("pendingTransactions", Collections.unmodifiableMap(pending));
        result.put("pendingOrphanBlocks", Collections.unmodifiableMap(orphans));
        result.put("rejectedBlocks", Collections.unmodifiableMap(rejected));
        return Collections.unmodifiableMap(result);
    }
    private static List<Block> walk(Blockchain chain) {
        List<Block> blocks = new ArrayList<>();
        Deque<Block> queue = new ArrayDeque<>(); queue.add(chain.getGenesis());
        while (!queue.isEmpty()) { Block block = queue.removeFirst(); blocks.add(block); queue.addAll(block.getNextBlocks()); }
        return blocks;
    }
}
