package AI.mcts.Optimazation.Heuristic;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;

public interface Heuristic {
    /**
     * Returns a score for this move in this state.
     * Higher is better.
     */
    double score(GameState state, Move move);
}
