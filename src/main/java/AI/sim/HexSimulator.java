// package AI.sim;
package AI.sim;

import AI.hex.HexMctsAdapter;
import AI.hex.HexState;
import AI.mcts.MCTS;
import AI.mcts.MctsArgs;
import AI.mcts.Outcome;

public final class HexSimulator {

    public static void main(String[] args) {
        int n = 7;                                // board size (start small)
        int games = 50;                           // number of test games
        MctsArgs strong = new MctsArgs(1.4, 800); // stronger agent
        MctsArgs weak   = new MctsArgs(1.4, 100); // weaker (or you can make a random agent)

        HexMctsAdapter game = new HexMctsAdapter(n);
        int mctsStrongerWins = 0, mctsWeakerWins = 0;

        for (int g = 0; g < games; g++) {
            boolean plusIsRedAtRoot = true; // choose who starts as you like; alternate if desired
            HexState s = new HexState(n, new int[n*n], plusIsRedAtRoot);

            // Two MCTS players with different search budgets
            MCTS<HexState> A = new MCTS<>(game, strong); // plays when it's +1’s turn
            MCTS<HexState> B = new MCTS<>(game, weak);

            int moves = 0;
            while (true) {
                // choose policy from the agent whose turn it is (always +1 perspective)
                double[] policy = (moves % 2 == 0) ? A.search(s) : B.search(s);

                // pick argmax action
                int best = 0;
                for (int a = 1; a < policy.length; a++) if (policy[a] > policy[best]) best = a;

                // apply
                s = game.getNextState(s, best, 1);
                Outcome o = game.valueAndTerminated(s, best);
                if (o.terminal) {
                    boolean plusWins = o.value > 0;    // +1 side (the player who just moved) wins
                    boolean strongerPlayedLast = (moves % 2 == 0); // A moves on even plies
                    if (plusWins == strongerPlayedLast) mctsStrongerWins++;
                    else mctsWeakerWins++;
                    break;
                }

                // hand over the turn (flip perspective)
                s = game.changePerspective(s, -1);
                moves++;
            }
        }

        System.out.printf("Stronger MCTS won %d / %d (%.1f%%)%n",
                mctsStrongerWins, games, 100.0 * mctsStrongerWins / games);
        System.out.printf("Weaker  MCTS won %d / %d (%.1f%%)%n",
                mctsWeakerWins, games, 100.0 * mctsWeakerWins / games);
    }
}
