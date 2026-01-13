
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.Heuristic.CentralityHeuristic;
import Game.Board;
import Game.Player;

class CentralityHeuristicTest {

    @Test
    void scoreTest_centerMoveGivesOne_onOddBoard() {
        Board b = new Board(5); // Center = (2,2)
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

        double s = h.score(state, new Move(2, 2));
        assertEquals(1.0, s, 1e-12);
    }

    @Test
    void scoreTest_cornerIsNearZero_onOddBoard() {
        Board b = new Board(5); // Center = (2,2)
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

        double s = h.score(state, new Move(0, 0));

        assertTrue(s >= 0.0);
        assertTrue(s < 1e-6, "Corner score should be ~0");
    }

    @Test
    void scoreTest_closerToCenterHigherScore() {
        Board b = new Board(5);
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

        double center = h.score(state, new Move(2, 2));
        double near   = h.score(state, new Move(2, 1)); 
        double far    = h.score(state, new Move(0, 0)); 

        assertTrue(center > near);
        assertTrue(near > far);
    }

    @Test
    void scoreTest_symmetry_sameDistanceSameScore_onOddBoard() {
        Board b = new Board(5); // Center (2,2)
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

        double top = h.score(state, new Move(0, 2));
        double bot = h.score(state, new Move(4, 2));

        assertEquals(top, bot, 1e-12);
    }

    @Test
    void scoreTest_evenBoard_hasTwoEqualBestCellsAroundCenter() {
        Board b = new Board(4); // Center = (1.5, 1.5)
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

        
        double s11 = h.score(state, new Move(1, 1));
        double s22 = h.score(state, new Move(2, 2));

        assertEquals(s11, s22, 1e-12);
        assertTrue(s11 < 1.0, "On even board any cell is exactly at center then score is < 1");
    }

    @Test
    void scoreTest_scoresInRangeForValidBoardCells() {
        Board b = new Board(6);
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

        double s1 = h.score(state, new Move(0, 0));
        double s2 = h.score(state, new Move(5, 5));
        double s3 = h.score(state, new Move(3, 2));

        assertTrue(s1 >= 0.0 && s1 <= 1.0);
        assertTrue(s2 >= 0.0 && s2 <= 1.0);
        assertTrue(s3 >= 0.0 && s3 <= 1.0);
    }

    @Test
    void scoreTest_outOfBoundsMove_canProduceNegativeScore_edgeCase() {
        Board b = new Board(5);
        GameState state = new GameState(b, Player.RED);
        CentralityHeuristic h = new CentralityHeuristic();

  
        double s = h.score(state, new Move(-10, -10));
        assertTrue(s < 0.0, "Out-of-bounds move may yield negative score (no validation)");
    }
}
