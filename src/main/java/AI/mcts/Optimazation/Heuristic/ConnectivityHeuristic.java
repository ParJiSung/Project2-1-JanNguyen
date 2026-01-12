package AI.mcts.Optimazation.Heuristic;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import Game.Board;
import Game.Color;
import Game.Player;

public final class ConnectivityHeuristic  implements Heuristic{
    @Override
    public double score(GameState state, Move move) {
        Board board = state.getBoard();
        Player toMove = state.getToMove();
        Color myColor = (toMove == Player.RED ? Color.RED : Color.BLACK);
        int friendly = 0;
        int total = 0;
        for (int[] rc : board.neighbors(move.row, move.col)) {
            int r = rc[0];
            int c = rc[1];
            total++;

            if (board.getCell(r, c) == myColor) friendly++;
        }
        return (total == 0) ? 0.0 : (double) friendly / total;
    }
}
