package jp.kota.bcasim.transaction;

import java.util.List;
import java.util.Random;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.event.GenerateTransaction;
import jp.kota.bcasim.main.event.SendTransaction;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.datastructure.Transaction;

/** A network-wide Poisson arrival process on its own random stream. */
public final class TransactionWorkload {
    private TransactionWorkload() { }
    public static void schedule(Simulation simulation) {
        if (!simulation.getConfig().isGenerateTransactions() || simulation.getConfig().getTransactionRate() == 0 ||
            simulation.getNetwork().getNodeList().size() < 2) return;
        double now = simulation.getScheduler().getSimulationTime();
        double interval = -Math.log(1.0 - simulation.getTransactionRandom().nextDouble()) / simulation.getConfig().getTransactionRate();
        double time = Math.max(Math.nextUp(now), now + interval);
        if (time <= simulation.getConfig().getSimulationTime())
            simulation.getScheduler().addNewEvent(new GenerateTransaction(time, simulation.getNetwork().getNodeList().get(0)));
    }
    public static void generate(Simulation simulation) {
        List<Node> nodes = simulation.getNetwork().getNodeList();
        Random random = simulation.getTransactionRandom();
        int from = random.nextInt(nodes.size());
        int to = random.nextInt(nodes.size() - 1);
        if (to >= from) to++;
        Node sender = nodes.get(from);
        LedgerState pending = sender.getTransactionPool().projectedState(sender.getBlockchain().getLatestBlock());
        int value = simulation.getConfig().getTransactionValue();
        if (pending.getBalance(sender.getName()) >= value) {
            Transaction transaction = simulation.createTransaction(sender.getName(), nodes.get(to).getName(), value,
                pending.getNextNonce(sender.getName()));
            simulation.getScheduler().addNewEvent(new SendTransaction(sender.now(), sender, transaction));
        }
        schedule(simulation);
    }
}
