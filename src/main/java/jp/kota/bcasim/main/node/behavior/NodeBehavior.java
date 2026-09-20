package jp.kota.bcasim.main.node.behavior;

import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.main.event.*;

/** One behavior instance per node. Transport and ledger mechanics remain on Node. */
public interface NodeBehavior {
    default void initialize(Node node) {}
    default void receiveBlock(Node node, ReceiveBlock event) {}
    default void foundBlock(Node node, FoundBlock event) {}
    default void receiveTransaction(Node node, ReceiveTransaction event) {}
    default void sendTransaction(Node node, SendTransaction event) {}
}
