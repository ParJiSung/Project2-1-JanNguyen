
import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.MovePruner;
import AI.mcts.Optimazation.Heuristic.Heuristic;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MovePrunerTest {

    //take score from the map otherwise we set 0.0
    static final class MapHeuristic implements Heuristic {
        private final Map<Move, Double> scores;

        MapHeuristic(Map<Move, Double> scores) {
            this.scores = scores;
        }

        @Override
        public double score(GameState state, Move m) {
            return scores.getOrDefault(m, 0.0);
        }
    }

    @Test
    void pruneMovesTest() {
        //threshold = 2.0, minMoves = 1
        //maxScore = 10.0, them min = 8.0 (the only moves that should be left are with 8.0 value)
        Move a = new Move(0, 0);
        Move b = new Move(0, 1);
        Move c = new Move(1, 0);

        Map<Move, Double> scores = new HashMap<>();
        scores.put(a, 10.0);
        scores.put(b, 9.0);
        scores.put(c, 7.9);

        MovePruner pruner = new MovePruner(2.0, 1, new MapHeuristic(scores));

        List<Move> legal = new ArrayList<>(List.of(a, b, c));
        List<Move> pruned = pruner.pruneMoves(null, legal);

        assertEquals(2, pruned.size());
        assertTrue(pruned.contains(a));
        assertTrue(pruned.contains(b));
        assertFalse(pruned.contains(c));
    }

    @Test
    void returnListTest() {
        MovePruner pruner = new MovePruner(1.0, 1, new MapHeuristic(Map.of()));

        List<Move> legal = new ArrayList<>();
        List<Move> result = pruner.pruneMoves(null, legal);

        assertSame(legal, result, "Empty list has to be returned");
        assertTrue(result.isEmpty());
    }

    @Test
    void largeThresholdTest() {
        Move a = new Move(0, 0);
        Move b = new Move(0, 1);
        Move c = new Move(1, 0);

        Map<Move, Double> scores = Map.of(
                a, 10.0,
                b, 5.0,
                c, -100.0
        );
        MovePruner pruner = new MovePruner(1000.0, 1, new MapHeuristic(scores));

        List<Move> legal = new ArrayList<>(List.of(a, b, c));
        List<Move> pruned = pruner.pruneMoves(null, legal);

        assertEquals(3, pruned.size());
        assertTrue(pruned.containsAll(legal));
    }

    @Test
    void smallThresholdTest() {
        Move a = new Move(0, 0);
        Move b = new Move(0, 1);
        Move c = new Move(1, 0);
        Move d = new Move(1, 1);

        Map<Move, Double> scores = new HashMap<>();
        scores.put(a, 10.0);
        scores.put(b, 1.0);
        scores.put(c, 0.5);
        scores.put(d, 0.4);

        MovePruner pruner = new MovePruner(0.0, 3, new MapHeuristic(scores));

        List<Move> legal = new ArrayList<>(List.of(d, c, b, a));
        List<Move> pruned = pruner.pruneMoves(null, legal);

        assertEquals(3, pruned.size());
        assertEquals(a, pruned.get(0));
        assertEquals(b, pruned.get(1));
        assertEquals(c, pruned.get(2));
    }

    @Test
    void returnSortedByScoreMovesTest() {
        Move a = new Move(0, 0);
        Move b = new Move(0, 1);

        Map<Move, Double> scores = Map.of(
                a, 2.0,
                b, 5.0
        );

        MovePruner pruner = new MovePruner(0.0, 10, new MapHeuristic(scores));

        List<Move> legal = new ArrayList<>(List.of(a, b));
        List<Move> pruned = pruner.pruneMoves(null, legal);

        assertEquals(2, pruned.size());
        assertEquals(b, pruned.get(0));
        assertEquals(a, pruned.get(1));
    }

    @Test
    void negativeThresholdTest() {
        Move a = new Move(0, 0);
        Move b = new Move(0, 1);
        Move c = new Move(1, 0);

        Map<Move, Double> scores = Map.of(
                a, 10.0,
                b, 9.0,
                c, 8.0
        );

        MovePruner pruner = new MovePruner(-1.0, 2, new MapHeuristic(scores));

        List<Move> legal = new ArrayList<>(List.of(c, b, a));
        List<Move> pruned = pruner.pruneMoves(null, legal);

        assertEquals(2, pruned.size());
        assertEquals(a, pruned.get(0));
        assertEquals(b, pruned.get(1));
    }

    @Test
    void equalScoresTest() {
        Move a = new Move(0, 0);
        Move b = new Move(0, 1);
        Move c = new Move(1, 0);

        Map<Move, Double> scores = Map.of(
                a, 1.0,
                b, 1.0,
                c, 1.0
        );

        MovePruner pruner = new MovePruner(0.0, 1, new MapHeuristic(scores));

        List<Move> legal = new ArrayList<>(List.of(a, b, c));
        List<Move> pruned = pruner.pruneMoves(null, legal);

        assertEquals(3, pruned.size(), "Если threshold=0 и все scores равны max, должны остаться все");
    }

    @Test
    void getThresholdTest() {
        MovePruner pruner = new MovePruner(3.14, 1, new MapHeuristic(Map.of()));
        assertEquals(3.14, pruner.getThreshold(), 0.0);
    }

    @Test
    void getHeuristicTest() {
        Heuristic h = new MapHeuristic(Map.of());
        MovePruner pruner = new MovePruner(1.0, 1, h);

        assertSame(h, pruner.getHeuristic());
    }
}
