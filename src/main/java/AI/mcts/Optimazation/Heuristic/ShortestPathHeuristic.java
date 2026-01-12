package AI.mcts.Optimazation.Heuristic;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;

public final class ShortestPathHeuristic implements Heuristic {

    @Override
    public double score(GameState state, Move move) {
        int before = state.estimateShortestPathForCurrentPlayer();
        GameState copy = state.copy();
        copy.doMove(move);
        int after = copy.estimateShortestPathForCurrentPlayer();
        int delta = before - after;
        return Math.tanh(delta / 3.0);
    }
}
