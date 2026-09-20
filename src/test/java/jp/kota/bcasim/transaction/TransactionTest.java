package jp.kota.bcasim.transaction;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import jp.kota.bcasim.configuration.SimulationConfig;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.event.SendTransaction;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.tool.fileio.ResultWriter;

public class TransactionTest {
    private SimulationConfig config() {
        return SimulationConfig.builder().seed(123).simulationTime(120).blockInterval(2)
            .nodeStrategies("honest", "honest").hashrates(1, 0).initialBalance(10)
            .transactionDelay(0).blockDelay(0).build();
    }
    private Block block(String hash, Block parent, Node miner, Transaction... transactions) {
        return new Block(hash, parent, parent.getTimestamp() + 1, miner, new ArrayList<>(Arrays.asList(transactions)));
    }
    private Transaction tx(String hash, String from, String to, int value, long nonce) {
        return new Transaction(from, to, value, nonce, hash);
    }
    @Test public void ledgerChecksNoncesBalanceAndRewardWithoutMutatingPreviousState() {
        LedgerState genesis = LedgerState.initial(2, 10);
        Transaction transfer = tx("a", "0", "1", 7, 0);
        LedgerState confirmed = genesis.applyBlock(Arrays.asList(transfer), "0", 3);
        assertEquals(10, genesis.getBalance("0"), 0);
        assertEquals(0, genesis.getNextNonce("0"));
        assertEquals(6, confirmed.getBalance("0"), 0);
        assertEquals(17, confirmed.getBalance("1"), 0);
        assertEquals(1, confirmed.getNextNonce("0"));
        assertFalse(confirmed.canApply(transfer));
        assertFalse(confirmed.canApply(tx("b", "0", "1", 7, 1)));
        assertFalse(confirmed.canApply(tx("c", "unknown", "1", 1, 0)));
        double[] snapshot = confirmed.getBalances(); snapshot[0] = 999;
        assertEquals(6, confirmed.getBalance("0"), 0);
    }
    @Test public void oldConstructorRetainsZeroNonceAndExplicitNegativeNonceIsRejected() {
        assertEquals(0, new Transaction("0", "1", 2, "old").getNonce());
        assertThrows(IllegalArgumentException.class, () -> tx("bad", "0", "1", 2, -1));
    }
    @Test public void poolDeduplicatesAndSelectsAffordableTransactionsInNonceOrder() {
        Simulation simulation = new Simulation(config().toBuilder().blockSize(2).blockReward(0).build());
        Node node = simulation.getNetwork().getNodeList().get(0);
        Transaction first = tx("first", "0", "1", 4, 0), next = tx("next", "0", "1", 4, 1);
        Transaction conflict = tx("conflict", "0", "1", 9, 0), overspend = tx("overspend", "0", "1", 3, 2);
        node.addTransaction(next); node.addTransaction(first); node.addTransaction(first);
        node.addTransaction(conflict); node.addTransaction(overspend);
        assertEquals(4, node.getTransactionPool().size());
        Block mined = node.generateNewBlock();
        assertEquals(Arrays.asList(first, next), mined.getTransactionList());
        assertEquals(2, mined.getLedgerState().getBalance("0"), 0);
        assertEquals(4, node.getTransactionPool().size());
        node.addNewBlock(mined);
        assertEquals(1, node.getTransactionPool().size());
        assertTrue(node.getTransactionPool().contains("overspend"));
        assertTrue(node.generateNewBlock().getTransactionList().isEmpty());
    }
    @Test public void invalidTransactionAdmissionDoesNotPropagateOrEnterPool() {
        Simulation simulation = new Simulation(config());
        Node node = simulation.getNetwork().getNodeList().get(0);
        node.sendTransaction(new SendTransaction(0, node, tx("huge", "0", "1", 11, 0)));
        node.sendTransaction(new SendTransaction(0, node, tx("account", "99", "1", 1, 0)));
        assertEquals(0, node.getTransactionPool().size());
        assertTrue(simulation.getScheduler().getEventList().isEmpty());
    }
    @Test public void transactionsPropagateToPeersExactlyOnceAndAreRemovedWhenConfirmed() {
        Simulation simulation = new Simulation(config());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Transaction transaction = simulation.createTransaction("0", "1", 4);
        a.sendTransaction(new SendTransaction(0, a, transaction));
        a.sendTransaction(new SendTransaction(0, a, transaction));
        simulation.getScheduler().processEvent();
        assertEquals(1, a.getTransactionPool().size());
        assertEquals(1, b.getTransactionPool().size());
        assertEquals(1, simulation.getScheduler().getProcessedEventCount());
        Block confirmed = block("confirmed", a.getBlockchain().getGenesis(), a, transaction);
        a.addNewBlock(confirmed); b.addNewBlock(confirmed);
        assertEquals(0, a.getTransactionPool().size());
        assertEquals(0, b.getTransactionPool().size());
        assertEquals(6 + config().getBlockReward(), b.getBlockchain().getLatestBlock().getLedgerState().getBalance("0"), 0);
    }
    @Test public void reorganizationRestoresDetachedTransactionsAndBalances() {
        Simulation simulation = new Simulation(config().toBuilder().blockReward(0).build());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block genesis = a.getBlockchain().getGenesis();
        Transaction transaction = tx("pay", "0", "1", 7, 0);
        a.addTransaction(transaction);
        Block paid = block("paid", genesis, a, transaction); a.addNewBlock(paid);
        assertEquals(0, a.getTransactionPool().size());
        Block alternative = block("alternative", genesis, b); a.addNewBlock(alternative);
        Block longer = block("longer", alternative, b); a.addNewBlock(longer);
        assertEquals("longer", a.getBlockchain().getLatestBlock().getHash());
        assertEquals(10, a.getBlockchain().getLatestBlock().getLedgerState().getBalance("0"), 0);
        assertTrue(a.getTransactionPool().contains("pay"));
        assertEquals(Arrays.asList(transaction), a.generateNewBlock().getTransactionList());
        assertEquals(1, a.getBlockchain().getReorgCount());
        assertEquals(1, a.getBlockchain().getMaxReorgDepth());
        assertEquals(1, a.getBlockchain().getForkPoints());
    }
    @Test public void aWinningConflictingBranchDoesNotRequeueSpentNonce() {
        Simulation simulation = new Simulation(config());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block genesis = a.getBlockchain().getGenesis();
        Transaction first = tx("first", "0", "1", 2, 0), conflicting = tx("conflicting", "0", "1", 3, 0);
        a.addNewBlock(block("old", genesis, a, first));
        Block alternative = block("other", genesis, b, conflicting); a.addNewBlock(alternative);
        a.addNewBlock(block("other2", alternative, b));
        assertFalse(a.getTransactionPool().contains("first"));
        assertEquals(1, a.getBlockchain().getLatestBlock().getLedgerState().getNextNonce("0"));
    }
    @Test public void dependentFundingTransactionsAreRestoredTogetherAfterReorganization() {
        Simulation simulation = new Simulation(config().toBuilder().blockReward(0).build());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block genesis = a.getBlockchain().getGenesis();
        Transaction funding = tx("funding", "0", "1", 10, 0), spending = tx("spending", "1", "0", 15, 0);
        a.addTransaction(funding); a.addTransaction(spending);
        assertEquals(2, a.getTransactionPool().size());
        a.addNewBlock(block("old", genesis, a, funding, spending));
        Block alternative = block("other", genesis, b); a.addNewBlock(alternative);
        a.addNewBlock(block("other2", alternative, b));
        assertEquals(2, a.getTransactionPool().size());
        assertEquals(Arrays.asList(funding, spending), a.generateNewBlock().getTransactionList());
    }
    @Test public void privateMiningUsesItsPrivateParentNonceAndDoesNotRepeatTransactions() {
        Simulation simulation = new Simulation(config().toBuilder().nodeStrategies("selfish", "honest").build());
        Node attacker = simulation.getNetwork().getNodeList().get(0);
        attacker.addTransaction(tx("pay", "0", "1", 3, 0));
        Block privateOne = attacker.generateNewBlock();
        assertEquals(1, privateOne.getTransactionList().size());
        Block privateTwo = attacker.generateNewBlock(privateOne);
        assertTrue(privateTwo.getTransactionList().isEmpty());
        assertEquals(1, privateTwo.getLedgerState().getNextNonce("0"));
    }
    @Test public void malformedReceivedBlocksAreRejectedBeforePropagationAndDoNotStopSimulation() {
        Simulation simulation = new Simulation(config());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block invalid = new Block("invalid", "00000000", 1, 1, a, new ArrayList<>(Arrays.asList(
            tx("spend", "0", "1", 7, 0), tx("again", "0", "1", 7, 0))));
        b.receiveBlock(new ReceiveBlock(1, b, invalid, a));
        assertEquals(1, b.getRejectedBlockCount());
        assertEquals(0, b.getBlockchain().getHeight());
        assertEquals(0, simulation.getBlockchain().getHeight());
        assertTrue(simulation.getScheduler().getEventList().isEmpty());
        Block valid = block("valid", b.getBlockchain().getGenesis(), a, tx("validpay", "0", "1", 7, 0));
        b.receiveBlock(new ReceiveBlock(1, b, valid, a));
        assertEquals(1, b.getBlockchain().getHeight());
    }
    @Test public void childReceivedBeforeParentIsDeduplicatedAndAcceptedAfterParent() {
        Simulation simulation = new Simulation(config());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block parent = block("parent", a.getBlockchain().getGenesis(), a, tx("pay", "0", "1", 4, 0));
        Block child = block("child", parent, a, tx("pay2", "0", "1", 3, 1));
        b.receiveBlock(new ReceiveBlock(1, b, child, a));
        b.receiveBlock(new ReceiveBlock(1, b, child, a));
        assertEquals(1, b.getOrphanBlockCount());
        b.receiveBlock(new ReceiveBlock(2, b, parent, a));
        assertEquals(0, b.getOrphanBlockCount());
        assertEquals(2, b.getBlockchain().getHeight());
        assertEquals(2, b.getBlockchain().getLatestBlock().getLedgerState().getNextNonce("0"));
    }
    @Test public void reconnectSyncsOnlyPublishedHistoryInParentFirstOrder() {
        Simulation simulation = new Simulation(config().toBuilder().adjacencyMatrix(new int[][]{{0, 0}, {0, 0}}).build());
        Node a = simulation.getNetwork().getNodeList().get(0), b = simulation.getNetwork().getNodeList().get(1);
        Block one = block("one", a.getBlockchain().getGenesis(), a); a.addNewBlock(one);
        Block two = block("two", one, a); a.addNewBlock(two);
        Block hidden = block("hidden", two, a); a.addUnpublishedBlocks(hidden);
        b.synchronizeWith(a);
        assertTrue(simulation.getScheduler().getEventList().isEmpty());
        simulation.getNetwork().addConnection(0, 1);
        b.synchronizeWith(a);
        simulation.getScheduler().processEvent();
        assertEquals(2, b.getBlockchain().getHeight());
        assertNull(b.getBlockchain().serchBlockHash("hidden"));
        assertEquals(0, b.getOrphanBlockCount());
    }
    private static final class TransactionTrace implements ResultWriter {
        final List<String> sends = new ArrayList<>();
        public void recordEvent(Event event) {
            if (event instanceof SendTransaction) {
                Transaction transaction = ((SendTransaction)event).getTransaction();
                sends.add(event.getEventTime() + ":" + transaction.getFrom() + ":" + transaction.getTo() + ":" + transaction.getNonce() + ":" + transaction.getTransactionHash());
            }
        }
    }
    @Test public void workloadIsReproducibleConservesValueAndConfirmsTransactions() {
        SimulationConfig settings = config().toBuilder().transactionRate(2).transactionValue(1).build();
        TransactionTrace first = new TransactionTrace(), repeat = new TransactionTrace();
        Simulation a = new Simulation(settings, first), b = new Simulation(settings, repeat);
        a.run(); b.run();
        assertFalse(first.sends.isEmpty());
        assertEquals(first.sends, repeat.sends);
        Block tip = a.getNetwork().getNodeList().get(1).getBlockchain().getLatestBlock();
        assertTrue(tip.getLedgerState().getNextNonce("0") + tip.getLedgerState().getNextNonce("1") > 0);
        double total = 0;
        for (double balance : tip.getLedgerState().getBalances()) { assertTrue(balance >= 0); total += balance; }
        assertEquals(20 + tip.getHeight() * settings.getBlockReward(), total, 0);
        assertEquals(tip.getHash(), b.getNetwork().getNodeList().get(1).getBlockchain().getLatestBlock().getHash());
    }
    @Test public void disabledWorkloadsDoNotGenerateTransactionsAndSingleNodeGenerationIsRejected() {
        for (SimulationConfig settings : Arrays.asList(config().toBuilder().transactionRate(0).build(),
            config().toBuilder().transactionRate(2).generateTransactions(false).build())) {
            TransactionTrace trace = new TransactionTrace(); new Simulation(settings, trace).run();
            assertTrue(trace.sends.isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> config().toBuilder().transactionRate(2)
            .hashrates(1).nodeStrategies("honest").adjacencyMatrix(new int[][]{{0}}).build());
    }
    @Test public void transactionCreationDoesNotConsumeMiningOrBlockIdentityRandomness() {
        Simulation baseline = new Simulation(config()), transactions = new Simulation(config());
        for (int i = 0; i < 20; i++) transactions.createTransaction("0", "1", 1);
        Block a = baseline.getNetwork().getNodeList().get(0).generateNewBlock();
        Block b = transactions.getNetwork().getNodeList().get(0).generateNewBlock();
        assertEquals(a.getHash(), b.getHash());
        assertEquals(a.getTimestamp(), b.getTimestamp(), 0);
    }
    @Test public void unfundedWorkloadRecordsAttemptsWithoutEmittingUnaffordableTransfers() {
        SimulationConfig settings = config().toBuilder().simulationTime(1).blockInterval(10000)
            .initialBalance(0).blockReward(0).transactionRate(100).build();
        TransactionTrace trace = new TransactionTrace();
        Simulation simulation = new Simulation(settings, trace);
        simulation.run();
        assertTrue(trace.sends.isEmpty());
        assertTrue(simulation.getScheduler().getProcessedEventCount() > 2);
    }
}
