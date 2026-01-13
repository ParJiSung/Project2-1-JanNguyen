import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.Heuristic.Heuristic;
import AI.mcts.Optimazation.Heuristic.LinearCombinationHeuristic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinearCombinationHeuristicTest {

    @Test
    void scoreTest_basicWeightedSum_withoutShortestPath() {
        Heuristic cent = (s, m) -> 2.0;
        Heuristic conn = (s, m) -> 3.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, null, 10.0, 1.0, 999.0);

        // wSP не должен влиять, т.к. shortestPath == null
        double score = h.score(null, new Move(0, 0));
        assertEquals(10.0 * 2.0 + 1.0 * 3.0, score, 0.0);
    }

    @Test
    void scoreTest_includesShortestPath_whenNotNullAndWeightNonZero() {
        Heuristic cent = (s, m) -> 1.0;
        Heuristic conn = (s, m) -> 2.0;
        Heuristic sp   = (s, m) -> 5.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, sp, 1.0, 1.0, 0.1);

        double score = h.score(null, new Move(1, 1));
        assertEquals(1.0 * 1.0 + 1.0 * 2.0 + 0.1 * 5.0, score, 1e-12);
    }

    @Test
    void scoreTest_doesNotCallShortestPath_whenWeightIsZero_edgeCase() {
        // если wSP == 0.0, shortestPath.score(...) вызываться не должен
        // сделаем эвристику shortestPath, которая бросает исключение при вызове
        Heuristic cent = (s, m) -> 1.0;
        Heuristic conn = (s, m) -> 1.0;
        Heuristic sp = (s, m) -> { throw new AssertionError("ShortestPath heuristic should not be called"); };

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, sp, 1.0, 1.0, 0.0);

        double score = h.score(null, new Move(0, 0));
        assertEquals(2.0, score, 0.0);
    }

    @Test
    void scoreTest_returnsZero_whenAllWeightsZero() {
        Heuristic cent = (s, m) -> 100.0;
        Heuristic conn = (s, m) -> -50.0;
        Heuristic sp   = (s, m) -> 999.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, sp, 0.0, 0.0, 0.0);

        assertEquals(0.0, h.score(null, new Move(2, 2)), 0.0);
    }

    @Test
    void scoreTest_supportsNegativeWeights_edgeCase() {
        Heuristic cent = (s, m) -> 2.0;
        Heuristic conn = (s, m) -> 3.0;
        Heuristic sp   = (s, m) -> 4.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, sp, -1.0, 2.0, -0.5);

        double score = h.score(null, new Move(0, 0));
        assertEquals((-1.0) * 2.0 + 2.0 * 3.0 + (-0.5) * 4.0, score, 1e-12);
    }

    @Test
    void scoreTest_linearProperty_scalingAllWeightsScalesScore() {
        Heuristic cent = (s, m) -> 1.5;
        Heuristic conn = (s, m) -> -2.0;
        Heuristic sp   = (s, m) -> 10.0;

        LinearCombinationHeuristic h1 =
                new LinearCombinationHeuristic(cent, conn, sp, 1.0, 2.0, 0.1);

        LinearCombinationHeuristic h2 =
                new LinearCombinationHeuristic(cent, conn, sp, 2.0, 4.0, 0.2);

        Move mv = new Move(1, 2);
        double s1 = h1.score(null, mv);
        double s2 = h2.score(null, mv);

        assertEquals(2.0 * s1, s2, 1e-12);
    }

    @Test
    void scoreTest_nanPropagates_edgeCase() {
        Heuristic cent = (s, m) -> Double.NaN;
        Heuristic conn = (s, m) -> 1.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, null, 1.0, 1.0, 0.0);

        double score = h.score(null, new Move(0, 0));
        assertTrue(Double.isNaN(score), "NaN from a component should propagate to final score");
    }

    @Test
    void scoreTest_infinityPropagates_edgeCase() {
        Heuristic cent = (s, m) -> Double.POSITIVE_INFINITY;
        Heuristic conn = (s, m) -> 1.0;
        Heuristic sp   = (s, m) -> 2.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, conn, sp, 1.0, 1.0, 1.0);

        double score = h.score(null, new Move(0, 0));
        assertTrue(Double.isInfinite(score));
        assertTrue(score > 0);
    }

    @Test
    void scoreTest_throwsNullPointer_whenCentralityIsNull_edgeCase() {
        Heuristic conn = (s, m) -> 1.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(null, conn, null, 1.0, 1.0, 0.0);

        assertThrows(NullPointerException.class, () -> h.score(null, new Move(0, 0)));
    }

    @Test
    void scoreTest_throwsNullPointer_whenConnectivityIsNull_edgeCase() {
        Heuristic cent = (s, m) -> 1.0;

        LinearCombinationHeuristic h =
                new LinearCombinationHeuristic(cent, null, null, 1.0, 1.0, 0.0);

        assertThrows(NullPointerException.class, () -> h.score(null, new Move(0, 0)));
    }
}

