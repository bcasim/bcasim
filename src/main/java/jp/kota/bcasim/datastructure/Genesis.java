package jp.kota.bcasim.datastructure;

import java.util.ArrayList;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.GenesisNode;
import jp.kota.bcasim.transaction.LedgerState;

public final class Genesis {
    private Genesis() {}
    public static Block getGenesis(Simulation simulation) {
        Block block = new Block("00000000", "none", 0, 0, new GenesisNode(simulation), new ArrayList<Transaction>());
        block.setLedgerState(LedgerState.initial(simulation.getConfig().getNumberOfNodes(), simulation.getConfig().getInitialBalance()));
        return block;
    }
}
