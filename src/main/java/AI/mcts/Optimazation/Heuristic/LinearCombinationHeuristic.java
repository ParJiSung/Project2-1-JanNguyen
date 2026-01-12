package AI.mcts.Optimazation.Heuristic;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;

public final class LinearCombinationHeuristic implements Heuristic {

    private final Heuristic centrality;
    private final Heuristic connectivity;
    private final Heuristic shortestPath; // can be null
    private final double wCenter;
    private final double wConn;
    private final double wSP;     // keep small (e.g. 0.1)

    public LinearCombinationHeuristic(Heuristic centrality,
                                      Heuristic connectivity,
                                      Heuristic shortestPath,
                                      double wCenter,
                                      double wConn,
                                      double wSP) {
        this.centrality = centrality;
        this.connectivity = connectivity;
        this.shortestPath = shortestPath;
        this.wCenter = wCenter;
        this.wConn = wConn;
        this.wSP = wSP;
    }

    @Override
    public double score(GameState state, Move move) {
        double score = 0.0;
        score += wCenter * centrality.score(state, move);
        score += wConn   * connectivity.score(state, move);

        if (shortestPath != null && wSP != 0.0) {
            score += wSP * shortestPath.score(state, move);
        }

        return score;
    }
}
