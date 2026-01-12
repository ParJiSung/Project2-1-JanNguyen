package AI.AiPlayer;

import AI.mcts.MCTS;
import AI.mcts.Node;
import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import AI.mcts.Optimazation.*;
import AI.mcts.Optimazation.Heuristic.*;
import AI.mcts.Steps.Expansion;
import AI.mcts.Steps.Selection;
import AI.mcts.Steps.SimulationStep.*;
import Game.Board;
import Game.Player;

/**
 * AIPlayer class that uses Monte Carlo Tree Search (MCTS) to determine the best move.
 * Integrates the AI system with the game.
 *
 * @author Team 04
 */
public class MCTSPlayer implements AIAgent {
    private final MCTS mcts;
    private final Player mctsPlayer;
    private final int iterations;

    //  agent pruning config
    private final double threshold;
    private final double centralityWeight;
    private final double connectivityWeight;

    /**
     * Constructor for Base MCTSPlayer
     * @param mctsPlayer The player this AI represents (RED or BLACK)
     * @param iterations The number of MCTS iterations to perform
     * @param threshold pruning threshold for this agent
     * @param centralityWeight weight used in heuristic
     * @param connectivityWeight weight used in heuristic
     */
    public MCTSPlayer(Player mctsPlayer, int iterations) {
        this.mctsPlayer = mctsPlayer;
        this.iterations = iterations;

        this.threshold = 0.0;
        this.centralityWeight = 0.0;
        this.connectivityWeight = 0.0;

        Selection selection = new Selection(Math.sqrt(2));
        Expansion expansion = new Expansion(null, null, 0.0);
        Simulation simulation = new BaseSimulation(); // pure random rollout

        this.mcts = new MCTS(iterations, selection, expansion, simulation);
    }

    /**
     * Constructor for Base MCTSPlayer
     * @param mctsPlayer The player this AI represents (RED or BLACK)
     * @param iterations The number of MCTS iterations to perform
     * @param threshold pruning threshold for this agent
     * @param centralityWeight weight used in heuristic
     * @param connectivityWeight weight used in heuristic
     */
    public MCTSPlayer(Player mctsPlayer, int iterations, double threshold, double centralityWeight,
                        double connectivityWeight, double biasScale, double spWeight, double cExploration) {

        this.mctsPlayer = mctsPlayer;
        this.iterations = iterations;
        this.threshold = threshold;
        this.centralityWeight = centralityWeight;
        this.connectivityWeight = connectivityWeight;

        // build combined heuristic with spWeight
        Heuristic centrality = new CentralityHeuristic();
        Heuristic connectivity = new ConnectivityHeuristic();
        Heuristic sp = new ShortestPathHeuristic();
        Heuristic combined = new LinearCombinationHeuristic(
                centrality,
                connectivity,
                sp,
                centralityWeight,
                connectivityWeight,
                spWeight
        );

        // pruner with combined heuristic
        int minMoves = 4;
        MovePruner pruner = new MovePruner(threshold, minMoves, combined);
        Selection selection = new Selection(cExploration);
        Expansion expansion = new Expansion(pruner, combined, biasScale);
        Simulation simulation = new BaseSimulation();
        this.mcts = new MCTS(iterations, selection, expansion, simulation);
    }

    @Override
    /**
     * Determines the best move for the AI player using MCTS.
     *
     * @param board The current game board state
     * @param currentPlayer The player whose turn it is
     * @return The best move found by the MCTS algorithm, or null if no moves are available
     */
    public Move getBestMove(Board board, Player currentPlayer) {
        GameState gameState = new GameState(board, currentPlayer);

        if (gameState.getLegalMoves().isEmpty()) {
            return null;
        }

        GameState simState = gameState.copy();
        Node root = new Node(null, null, currentPlayer.other().id);
        Node bestNode = mcts.search(root, simState);

        if (bestNode == null || bestNode.move == null) {
            return gameState.getLegalMoves().get(0);
        }

        return bestNode.move;
    }

    @Override
    /**
     * Gets the player this AI represents.
     * @return The player (RED or BLACK)
     */
    public Player getPlayer() {
        return mctsPlayer;
    }

    @Override
    /**
     * Checks if the given player is controlled by this AI.
     * @param player The player to check
     * @return true if this AI controls the player, false otherwise
     */
    public boolean controlsPlayer(Player player) {
        return this.mctsPlayer == player;
    }

    /**
     * Gets the number of iterations used in MCTS search.
     * @return The iteration count
     */
    public int getIterations() {
        return iterations;
    }

    // getter for reporting
    public MovePruner getPruner() {
        return mcts.getPruner();
    }

    public double getThreshold() {
        return threshold;
    }

    public double getCentralityWeight() {
        return centralityWeight;
    }

    public double getConnectivityWeight() {
        return connectivityWeight;
    }
}
