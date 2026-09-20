package jp.kota.bcasim.datastructure;

import java.util.ArrayList;
import jp.kota.bcasim.main.Simulation;
import jp.kota.bcasim.main.node.GenesisNode;

public final class Genesis {
    private Genesis() {}
    public static Block getGenesis(Simulation simulation) {
        return new Block("00000000", "none", 0, 0, new GenesisNode(simulation), new ArrayList<Transaction>());
    }
}
