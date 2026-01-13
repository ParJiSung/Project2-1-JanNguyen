package AI.AlphaZero;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.nd4j.linalg.api.ndarray.INDArray;

import AI.mcts.HexGame.Move;
import AI.mcts.Node;
import Game.Board;
import Game.Color;

public class AlphaZeroMCTS {
    private AlphaZeroNet network; // The neural network used for evaluations
    private final double C_PUCT = Math.sqrt(2); // Exploration constant
    private final double DIR_EPS = 0.25;  // ε
    private final double DIR_ALPHA = 0.10; // α for 11x11 Hex

    public AlphaZeroMCTS(AlphaZeroNet network) {
        this.network = network;
    }

    // Main search function, which is called when it is the AI's turn to move.
    public Node search(Board rootBoard, Color rootPlayer, int iterations) {
        // Create root node
        Node root = new Node(null, null, rootPlayer == Color.RED ? 1 : 2);
        
        // EXPAND ROOT immediately using the network
        expandAndEvaluate(root, rootBoard, rootPlayer);
        addDirichletNoiseToRoot(root);

        // Perform Simulations
        for (int i = 0; i < iterations; i++) {
            Node node = root;
            Board copiedBoard = rootBoard.copyBoard(rootBoard); // Create a copy of the board to simulate on
            Color currentPlayer = rootPlayer;

            // This selection loop goes down the tree that is already built and uses the PUCT formula for the selection of moves/nodes.
            while (!node.children.isEmpty()) {
                node = selectBestChild(node);
                // Apply the move to the copied board.
                if (currentPlayer == Color.RED) copiedBoard.getMoveRed(node.move.row, node.move.col, null);
                else copiedBoard.getMoveBlack(node.move.row, node.move.col, null);
                currentPlayer = (currentPlayer == Color.RED) ? Color.BLACK : Color.RED; // Switch turn
            }

            // Expansion and evalution
            double value;
            // If the game isn't over, ask the network for value/policy
            if (!copiedBoard.isTerminal()) {
                value = expandAndEvaluate(node, copiedBoard, currentPlayer);
            } else {
                // If game over, value is simple: 1 if we won, -1 if we lost
                boolean redWon = copiedBoard.redWins();
                // If I am RED and Red won -> +1. If I am RED and Red lost -> -1.
                value = (redWon && currentPlayer == Color.RED) ? 1.0 : -1.0; 
                if (!redWon && currentPlayer == Color.BLACK) value = 1.0; 
            }

            // Backpropagation
            // Propagate the value up the tree
            // Note: Value is from the perspective of the player who JUST moved.
            // So we flip the sign at each level.
            backpropagate(node, value);
        }

        return root;
    }

    private void backpropagate(Node node, double value) {
        while (node != null) {
            node.visits++;
            node.wins += value; 
            value = -value; // Flip perspective for the opponent
            node = node.parent;
        }
    }

    private double expandAndEvaluate(Node node, Board board, Color player) {
        // Encode the board state for the neural network
        INDArray input = BoardEncoder.encode(board, player);
        
        // Ask the Network
        INDArray[] output = network.getModel().output(input);

        INDArray policy = output[0]; // Move probabilities
        double value = output[1].getDouble(0); // Win chance (-1 to 1)

        // Create Children
        List<int[]> legalMoves = board.legalMoves(); // List of legal moves as (row, col) pairs
        
        double policySum = 0; // For normalization (if needed), and should sum up to 1.

        for (int[] move : legalMoves) {
            int row = move[0];
            int col = move[1];
            
            // Map 2D (row, col) to 1D index for policy array
            int idx = row * board.getSize() + col;
            double prob = policy.getDouble(idx);
            
            Move newMove = new Move(row, col); // Create a new Move object
            Node child = new Node(newMove, node, 0); // Need to fix player ID passed here
            child.priorProbability = prob; // Set the Prior for this node.
            
            node.children.put(newMove, child);
            policySum += prob;
        }

        if (policySum > 0) {
            for (Node child : node.children.values()) {
                child.priorProbability /= policySum;
            }
        } else {
            double uniform = 1.0 / node.children.size();
            for (Node child : node.children.values()) child.priorProbability = uniform;
        }

        return value;
    }

    // PUCT Formula for Selection
    private Node selectBestChild(Node parent) {
        Node bestChild = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Node child : parent.children.values()) {
            // Calculate the average win rate of this move so far, if visited.
            double Q = (child.visits > 0) ? (child.wins / child.visits) : 0;

            // Exploration term
            double U = C_PUCT * child.priorProbability * (Math.sqrt(parent.visits) / (1 + child.visits));
            
            // Calculate the total score
            double score = Q + U; 

            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }
        return bestChild;
    }

    /**
     * Returns the probability distribution (Policy) based on visit counts.
     * @param root The root node after the search is finished.
     * @param temperature Controls exploration. 
     * temperature=1.0: Probabilities are proportional to visits (Exploratory).
     * temperature=0.0: The move with max visits gets probability 1.0 (Competitive).
     * @param boardSize The size of the board (e.g., 11 for 11x11).
     * @return A double array of size boardSize*boardSize representing move probabilities.
     */
    public double[] getSearchPolicy(Node root, double temperature, double boardSize) {
        double[] policy = new double[(int)(boardSize*boardSize)]; // Initialize all to 0.0
        
        // If temperature is close to 0, just pick the max visited node (competitive play), because this move is most likely to lead to a win.
        if (temperature < 0.01) {
            int bestIdx = -1;
            int maxVisits = -1;
            for (Map.Entry<Move, Node> entry : root.children.entrySet()) {
                if (entry.getValue().visits > maxVisits) {
                    maxVisits = entry.getValue().visits;
                    Move m = entry.getKey();
                    bestIdx = (int)(m.row * boardSize + m.col); // Map 2D -> 1D
                }
            }
            if (bestIdx != -1) policy[bestIdx] = 1.0;
            return policy;
        }

        // Otherwise, calculate probabilities: (visits)^(1/temperature). (Exploratory play) This is done early on in the game, since the AI needs to try different openings of the game. If the temperature would be high, the AI would play the exact same opening over and over again, which will cause the AI to not explore other moves which could turn out to be better for the future.
        double sum = 0;
        for (Map.Entry<Move, Node> entry : root.children.entrySet()) {
            Move m = entry.getKey();
            Node child = entry.getValue();
            
            double visits = Math.pow(child.visits, 1.0 / temperature);
            int idx = (int)(m.row * boardSize + m.col); // Map 2D -> 1D
            policy[idx] = visits;
            sum += visits;
        }

        // Normalize so they sum to 1.0
        for (int i = 0; i < policy.length; i++) {
            policy[i] /= sum;
        }
        
        return policy;
    }

    private void addDirichletNoiseToRoot(Node root) {
        int k = root.children.size();
        if (k == 0) return;

        GammaDistribution gamma = new GammaDistribution(DIR_ALPHA, 1.0);

        double[] noise = new double[k];
        double sum = 0.0;

        for (int i = 0; i < k; i++) {
            double x = gamma.sample();
            noise[i] = x;
            sum += x;
        }

        if (sum <= 0.0) {
            double uniform = 1.0 / k;
            for (int i = 0; i < k; i++) noise[i] = uniform;
        } else {
            for (int i = 0; i < k; i++) noise[i] /= sum;
        }

        var children = new ArrayList<>(root.children.values());

        double priorSum = 0.0;
        for (int i = 0; i < k; i++) {
            Node child = children.get(i);
            child.priorProbability = (1 - DIR_EPS) * child.priorProbability + DIR_EPS * noise[i];
            priorSum += child.priorProbability;
        }

        if (priorSum > 0.0) {
            for (Node child : children) child.priorProbability /= priorSum;
        }
    }
}