
import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.Heuristic.ShortestPathHeuristic;
import Game.Board;
import Game.Color;
import Game.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortestPathHeuristicTest {

    private static void place(Board board, int r, int c, Color color) {
        if (color == Color.RED) {
            board.getMoveRed(r, c, Color.RED);
        } else if (color == Color.BLACK) {
            board.getMoveBlack(r, c, Color.BLACK);
        } else {
            throw new IllegalArgumentException("Only RED/BLACK allowed");
        }
    }

    @Test
    void scoreTest_returnsZero_whenDeltaIsZero_symmetricEmptyBoard() {
        Board b = new Board(3);
        GameState state = new GameState(b, Player.RED);

        ShortestPathHeuristic h = new ShortestPathHeuristic();
        double s = h.score(state, new Move(1, 1));

        assertEquals(0.0, s, 1e-12);
    }

    @Test
    void scoreTest_negative_whenCurrentPlayerHasShorterPathThanNextPlayer() {
        Board b = new Board(3);

        place(b, 0, 1, Color.RED);
        place(b, 1, 1, Color.RED);

        GameState state = new GameState(b, Player.RED);
        ShortestPathHeuristic h = new ShortestPathHeuristic();

        double s = h.score(state, new Move(2, 2));

        assertTrue(s < 0.0, "Expected negative score because before(RED) < after(BLACK) with this setup");
        assertTrue(s > -1.0 && s < 1.0, "tanh output must be in (-1,1)");
    }

    @Test
    void scoreTest_positive_whenCurrentPlayerHasLongerPathThanNextPlayer() {
        Board b = new Board(3);
        place(b, 0, 1, Color.RED);
        place(b, 1, 1, Color.RED);

        GameState state = new GameState(b, Player.BLACK);
        ShortestPathHeuristic h = new ShortestPathHeuristic();
        double s = h.score(state, new Move(2, 2));

        assertTrue(s > 0.0, "Expected positive score because before(BLACK) > after(RED) with this setup");
        assertTrue(s > -1.0 && s < 1.0, "tanh output must be in (-1,1)");
    }

    @Test
    void scoreTest_returnsZero_forInvalidMove_edgeCase() {
        Board b = new Board(3);
        place(b, 1, 1, Color.RED);

        GameState state = new GameState(b, Player.RED);
        ShortestPathHeuristic h = new ShortestPathHeuristic();

        
        double s = h.score(state, new Move(1, 1));

        assertEquals(0.0, s, 1e-12);
    }

    @Test
    void scoreTest_returnsZero_whenStateIsTerminal_edgeCase() {
        Board b = new Board(1);
        place(b, 0, 0, Color.RED); 

        GameState state = new GameState(b, Player.RED);
        assertTrue(state.isTerminal(), "Precondition is true");

        ShortestPathHeuristic h = new ShortestPathHeuristic();

        
        double s = h.score(state, new Move(0, 0));

        assertEquals(0.0, s, 1e-12);
    }

    @Test
    void scoreTest_doesNotMutateOriginalState_orBoard() {
        Board b = new Board(3);
        GameState state = new GameState(b, Player.RED);

        Color beforeCell = b.getCell(1, 1);
        Player beforeToMove = state.getToMove();

        ShortestPathHeuristic h = new ShortestPathHeuristic();
        h.score(state, new Move(1, 1));

       
        assertEquals(beforeCell, b.getCell(1, 1));
        assertEquals(beforeToMove, state.getToMove());
    }

    @Test
    void scoreTest_throwsNullPointer_forNullMove_edgeCase() {
        Board b = new Board(3);
        GameState state = new GameState(b, Player.RED);
        ShortestPathHeuristic h = new ShortestPathHeuristic();

        assertThrows(NullPointerException.class, () -> h.score(state, null));
    }

    @Test
    void scoreTest_resultIsFinite_forNormalInputs() {
        Board b = new Board(3);
        GameState state = new GameState(b, Player.RED);
        ShortestPathHeuristic h = new ShortestPathHeuristic();

        double s = h.score(state, new Move(0, 0));

        assertTrue(Double.isFinite(s), "Score should be finite");
        assertTrue(s > -1.0 && s < 1.0, "tanh output must be in (-1,1)");
    }
}

