package AI;

import AI.hex.HexMctsAdapter;
import AI.hex.HexState;
import AI.mcts.MCTS;
import AI.mcts.MctsArgs;

public class RunMctsDemo {
    public static void main(String[] args) {
        int n = 11;
        HexMctsAdapter game = new HexMctsAdapter(n);
        MctsArgs mctsArgs = new MctsArgs(1.4, 800);

        // empty canonical state (all zeros, +1 plays Red by default)
        int[] cells = new int[n * n];
        HexState root = new HexState(n, cells, /*plusPlayerIsRed=*/true);

        MCTS<HexState> mcts = new MCTS<>(game, mctsArgs);
        double[] policy = mcts.search(root);

        int bestAction = 0;
        for (int a = 1; a < policy.length; a++) {
            if (policy[a] > policy[bestAction]) bestAction = a;
        }
        int row = bestAction / n, col = bestAction % n;
        System.out.println("MCTS suggests: " + row + ", " + col);
    }
}
