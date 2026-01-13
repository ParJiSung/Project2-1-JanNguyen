import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.Heuristic.ConnectivityHeuristic;
import Game.Board;
import Game.Color;
import Game.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectivityHeuristicTest {
    //initial state
    private static void place(Board board, int r, int c, Color color) {
        if (color == Color.RED) {
            board.getMoveRed(r, c, Color.RED);
        } else if (color == Color.BLACK) {
            board.getMoveBlack(r, c, Color.BLACK);
        } else {
            throw new IllegalArgumentException("Only RED or BLACK allowed");
        }
    }

    @Test
    void scoreTest_returnsZero_whenNoNeighborsExist_size1() {
        Board b = new Board(1);
        GameState s = new GameState(b, Player.RED);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

        double score = h.score(s, new Move(0, 0));

        assertEquals(0.0, score, 0.0);
    }

    @Test
    void scoreTest_returnsZero_whenNoFriendlyNeighbors() {
        Board b = new Board(3);
        GameState s = new GameState(b, Player.RED);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

        // вокруг (1,1) никого нет
        double score = h.score(s, new Move(1, 1));

        assertEquals(0.0, score, 0.0);
    }

    @Test
    void scoreTest_centerCell_countsFriendlyFraction_forRedToMove() {
        Board b = new Board(3);
        GameState s = new GameState(b, Player.RED);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

        place(b, 0, 1, Color.RED);
        place(b, 1, 0, Color.RED);
        place(b, 2, 1, Color.RED);

        double score = h.score(s, new Move(1, 1));

        assertEquals(0.5, score, 1e-12);
    }

    @Test
    void scoreTest_centerCell_allNeighborsFriendly_returnsOne() {
        Board b = new Board(3);
        GameState s = new GameState(b, Player.RED);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

        place(b, 0, 1, Color.RED);
        place(b, 0, 2, Color.RED);
        place(b, 1, 0, Color.RED);
        place(b, 1, 2, Color.RED);
        place(b, 2, 0, Color.RED);
        place(b, 2, 1, Color.RED);

        double score = h.score(s, new Move(1, 1));

        assertEquals(1.0, score, 1e-12);
    }

    @Test
    void scoreTest_cornerCell_totalNeighborsLessThanSix_andFractionCorrect() {
        Board b = new Board(3);
        GameState s = new GameState(b, Player.RED);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

    
        int total = b.neighbors(0, 0).size();
        assertTrue(total > 0 && total < 6, "Corner should have fewer neighbors than center");

        
        int[] first = b.neighbors(0, 0).get(0);
        place(b, first[0], first[1], Color.RED);

        double score = h.score(s, new Move(0, 0));

        assertEquals(1.0 / total, score, 1e-12);
    }

    @Test
    void scoreTest_usesToMoveColor_blackToMoveCountsBlackNeighborsNotRed() {
        Board b = new Board(3);
        GameState s = new GameState(b, Player.BLACK);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

        place(b, 0, 1, Color.BLACK);
        place(b, 1, 0, Color.BLACK);

        place(b, 2, 1, Color.RED);
        place(b, 1, 2, Color.RED);

        int total = b.neighbors(1, 1).size(); 
        double expected = 2.0 / total; 

        double score = h.score(s, new Move(1, 1));

        assertEquals(expected, score, 1e-12);
    }

    @Test
    void scoreTest_sameBoardDifferentToMove_changesScore() {
        Board b = new Board(3);
        ConnectivityHeuristic h = new ConnectivityHeuristic();

        place(b, 0, 1, Color.RED);
        place(b, 1, 0, Color.BLACK);

        int total = b.neighbors(1, 1).size();

        GameState redState = new GameState(b, Player.RED);
        GameState blackState = new GameState(b, Player.BLACK);

        double redScore = h.score(redState, new Move(1, 1));    
        double blackScore = h.score(blackState, new Move(1, 1)); 

        assertEquals(1.0 / total, redScore, 1e-12);
        assertEquals(1.0 / total, blackScore, 1e-12);
        
    }
}

