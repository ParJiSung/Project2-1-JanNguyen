package AI.mcts.Optimazation.Heuristic;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import Game.Board;

public final class CentralityHeuristic  implements Heuristic{

    @Override
    public double score(GameState state, Move move) {
        Board board = state.getBoard();
        int n = board.getSize();
        double centerRow = (n - 1) / 2.0;
        double centerCol = (n - 1) / 2.0;
        double distCenter = Math.hypot(move.row - centerRow, move.col - centerCol);
        double maxDist = Math.hypot(centerRow, centerCol);
        return 1.0 - distCenter / (maxDist + 1e-9);
    }
}
