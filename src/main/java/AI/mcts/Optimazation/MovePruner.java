package AI.mcts.Optimazation;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.Heuristic.*;

import java.util.*;

public class MovePruner {

    //set thershold to represent how many moves we want to keep (smaller=fewer)
    private final double threshold;

    // heuristics
    private final int minMoves;
    private final Heuristic heuristic;

    //  constructor to allow different agents to use different pruning settings (used for testing)
    public MovePruner(double threshold, int minMoves, Heuristic heuristic) {
        this.threshold = threshold;
        this.minMoves = minMoves;
        this.heuristic = heuristic;
    }


    //scores each move based on the heuristic, find max among legal (possible) moves and creates a minimum score for being kept
    public List<Move> pruneMoves(GameState state, List<Move> legalMoves) {
        if (legalMoves.isEmpty()) return legalMoves;

        Map<Move, Double> scores = new HashMap<>();
        double maxScore = Double.NEGATIVE_INFINITY;

        for (Move m : legalMoves) {
            double s = heuristic.score(state, m);
            scores.put(m, s);
            if (s > maxScore) maxScore = s;
        }

        double min = maxScore - threshold;
        List<Move> pruned = new ArrayList<>();
        for (Move m : legalMoves) {
            if (scores.get(m) >= min) {
                pruned.add(m);
            }
        }

        if (pruned.size() < minMoves) {
            legalMoves.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
            pruned.clear();
            for (int i = 0; i < Math.min(minMoves, legalMoves.size()); i++) {
                pruned.add(legalMoves.get(i));
            }
        }
        return pruned;
    }

    public double getThreshold() {
        return threshold;
    }

    public Heuristic getHeuristic() {
        return heuristic;
    }
}
