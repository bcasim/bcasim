package jp.kota.bcasim.tool.fileio;

import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.event.ReceiveTransaction;
import jp.kota.bcasim.main.event.SendTransaction;
import jp.kota.bcasim.main.node.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps domain objects to the existing visualizer schema. */
final class ResultRecords {
    private ResultRecords() { }

    static Map<String, Object> block(Block block, double blockReward) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("hash", block.getHash());
        record.put("previousHash", block.getPreviousHash());
        record.put("timestamp", String.valueOf(block.getTimestamp()));
        record.put("receiveTime", String.valueOf(block.getReceiveBlockTime()));
        record.put("height", String.valueOf(block.getHeight()));
        record.put("miner", name(block.getMiner()));
        record.put("reward", String.valueOf(blockReward));
        List<String> transactions = new ArrayList<>();
        for (Transaction transaction : block.getTransactionList()) {
            transactions.add(transaction.getTransactionHash());
        }
        record.put("transaction", transactions);
        return record;
    }

    static Map<String, Object> event(Event event) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("eventID", String.valueOf(event.getEventID()));
        record.put("time", String.valueOf(event.getEventTime()));
        record.put("type", event.getEventType());
        record.put("node", name(event.getNode()));
        if (event instanceof jp.kota.bcasim.main.event.NetworkChange) {
            jp.kota.bcasim.main.event.NetworkChange change = (jp.kota.bcasim.main.event.NetworkChange) event;
            record.put("from", String.valueOf(change.getFromIndex()));
            record.put("to", String.valueOf(change.getToIndex()));
            record.put("action", change.getAction());
        } else if (event instanceof ReceiveBlock) {
            ReceiveBlock received = (ReceiveBlock) event;
            addBlock(record, received.getBlock());
            record.put("from", name(received.getFrom()));
        } else if (event instanceof FoundBlock) {
            addBlock(record, ((FoundBlock) event).getBlock());
        } else if (event instanceof ReceiveTransaction) {
            ReceiveTransaction received = (ReceiveTransaction) event;
            addTransaction(record, received.getTransaction());
            record.put("from", name(received.getFrom()));
        } else if (event instanceof SendTransaction) {
            addTransaction(record, ((SendTransaction) event).getTransaction());
        }
        return record;
    }

    private static void addBlock(Map<String, Object> record, Block block) {
        record.put("height", String.valueOf(block.getHeight()));
        record.put("miner", name(block.getMiner()));
        record.put("hash", block.getHash());
    }

    private static void addTransaction(Map<String, Object> record, Transaction transaction) {
        record.put("hash", transaction.getTransactionHash());
        record.put("accountFrom", transaction.getFrom());
        record.put("accountTo", transaction.getTo());
        record.put("value", transaction.getValue());
        record.put("nonce", transaction.getNonce());
    }

    private static String name(Node node) {
        return node == null ? "" : node.getName();
    }
}
