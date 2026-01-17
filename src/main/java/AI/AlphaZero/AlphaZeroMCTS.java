package AI.AlphaZero;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.math3.distribution.GammaDistribution;

import AI.mcts.HexGame.Move;
import AI.mcts.Node;
import Game.Board;
import Game.Color;

public class AlphaZeroMCTS {
    private Batcher batcher; 
    private int boardSize;
    
    private final double C_PUCT = Math.sqrt(2);
    private final double DIR_EPS = 0.25;
    private final double DIR_ALPHA = 0.10;

    private volatile boolean trainingMode = true;
    public void setTrainingMode(boolean trainingMode) { this.trainingMode = trainingMode; }
    
    // OPTIMIZATION: Reuse Gamma distribution (thread-safe for sampling)
    private final GammaDistribution gammaDist = new GammaDistribution(DIR_ALPHA, 1.0); 

    public AlphaZeroMCTS(Batcher batcher, int boardSize) {
        this.batcher = batcher;
        this.boardSize = boardSize;
    }

    public Node search(Board rootBoard, Color rootPlayer, int iterations) {
        // 1. Create Root
        Node root = new Node(null, null, rootPlayer == Color.RED ? 1 : 2);
        
        // 2. Expand Root (First evaluation) - cache encoding for training data reuse
        expandAndEvaluate(root, rootBoard, rootPlayer);
        if (trainingMode) addDirichletNoiseToRoot(root);

        // 3. Simulation Loop - use undo/redo instead of copying
        // Create a working board that we'll modify and undo
        // Note: fastCopy() creates a new Board with empty moveHistory, so no need to clear
        Board workingBoard = rootBoard.fastCopy();

        for (int i = 0; i < iterations; i++) {
            Node node = root;
            Color currentPlayer = rootPlayer;
            
            // Track moves made in this iteration for undo
            int movesMade = 0;

            // Traverse down the tree (Selection)
            while (!node.children.isEmpty()) {
                node = selectBestChild(node);
                
                // Apply move to working board
                if (currentPlayer == Color.RED) {
                    workingBoard.getMoveRed(node.move.row, node.move.col, null);
                } else {
                    workingBoard.getMoveBlack(node.move.row, node.move.col, null);
                }
                movesMade++;
                currentPlayer = (currentPlayer == Color.RED) ? Color.BLACK : Color.RED;
            }

            // Expansion & Evaluation
            double value;
            if (!workingBoard.isTerminal()) {
                value = expandAndEvaluate(node, workingBoard, currentPlayer);
            } else {
                boolean redWon = workingBoard.redWins();
                // Value is from the perspective of the *current* player at this leaf
                if (redWon) {
                    value = (currentPlayer == Color.RED) ? 1.0 : -1.0;
                } else {
                    value = (currentPlayer == Color.BLACK) ? 1.0 : -1.0; 
                }
            }

            // Backpropagation
            backpropagate(node, value);
            
            // Undo all moves made in this iteration to restore working board
            for (int j = 0; j < movesMade; j++) {
                workingBoard.undoMove();
            }
        }

        return root;
    }

    private void backpropagate(Node node, double value) {
        while (node != null) {
            node.visits++;
            node.wins += value; 
            value = -value; // Flip perspective for parent
            node = node.parent;
        }
    }

    private double expandAndEvaluate(Node node, Board board, Color player) {
        // Encode board for Neural Net
        float[] input = BoardEncoder.encode(board, player);
        
        // Cache encoding in root node for training data reuse (only for root)
        if (node.parent == null && node.cachedEncoding == null) {
            node.cachedEncoding = input.clone(); // Clone to avoid mutation
        }
        
        // ASYNC INFERENCE via Batcher
        NeuralNetBatcher.Output output;
        try {
            // This .get() will block, but the Batcher ensures we wait efficiently
            output = batcher.predict(input).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error during Neural Net Inference", e);
        }

        float[] policy = output.policy;
        double value = output.value;

        // Expand children
        List<int[]> legalMoves = board.legalMoves();
        double policySum = 0;

        for (int[] move : legalMoves) {
            int row = move[0];
            int col = move[1];
            int idx = row * boardSize + col;
            
            double prob = policy[idx];
            
            Move newMove = Move.get(row, col);
            Node child = new Node(newMove, node, 0);
            child.priorProbability = prob;
            
            node.children.put(newMove, child);
            policySum += prob;
        }

        // Normalize probabilities
        if (policySum > 0) {
            for (Node child : node.children.values()) {
                child.priorProbability /= policySum;
            }
        } else {
            // Fallback if network outputs all zeros (rare)
            double uniform = 1.0 / Math.max(1, node.children.size());
            for (Node child : node.children.values()) child.priorProbability = uniform;
        }

        return value;
    }

    private Node selectBestChild(Node parent) {
        Node bestChild = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        double sqrtParentVisits = Math.sqrt(parent.visits + 1.0);

        for (Node child : parent.children.values()) {
            // child.wins is from CHILD (opponent-to-move) perspective because backprop flips sign each level.
            // Parent should therefore NEGATE it when choosing.
            double Q = (child.visits > 0) ? -(child.wins / child.visits) : 0.0;

            double U = C_PUCT * child.priorProbability * (sqrtParentVisits / (1.0 + child.visits));
            double score = Q + U;

            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }
        return bestChild;
    }


    public double[] getSearchPolicy(Node root, double temperature) {
        int size = boardSize * boardSize;
        double[] policy = new double[size];

        // Greedy (Temperature ~ 0)
        if (temperature < 0.01) {
            int bestIdx = -1;
            int maxVisits = -1;
            for (Map.Entry<Move, Node> entry : root.children.entrySet()) {
                if (entry.getValue().visits > maxVisits) {
                    maxVisits = entry.getValue().visits;
                    Move m = entry.getKey();
                    bestIdx = m.row * boardSize + m.col;
                }
            }
            if (bestIdx != -1) policy[bestIdx] = 1.0;
            return policy;
        }

        // Exploration (Temperature > 0)
        double sum = 0;
        for (Map.Entry<Move, Node> entry : root.children.entrySet()) {
            Move m = entry.getKey();
            Node child = entry.getValue();

            double visits = Math.pow(child.visits, 1.0 / temperature);
            int idx = m.row * boardSize + m.col;
            policy[idx] = visits;
            sum += visits;
        }

        // ===== STEP 2 GOES HERE =====
        if (sum <= 0.0) {
            double uniform = 1.0 / Math.max(1, root.children.size());
            for (Map.Entry<Move, Node> entry : root.children.entrySet()) {
                Move m = entry.getKey();
                int idx = (int)(m.row * boardSize + m.col);
                policy[idx] = uniform;
            }
            return policy;
        }
        // ============================

        for (int i = 0; i < size; i++) {
            policy[i] /= sum;
        }
        return policy;
    }

    private void addDirichletNoiseToRoot(Node root) {
        int k = root.children.size();
        if (k == 0) return;

        // OPTIMIZATION: Reuse cached gamma distribution instead of creating new one
        double[] noise = new double[k];
        double sum = 0.0;

        for (int i = 0; i < k; i++) {
            noise[i] = gammaDist.sample(); // Use cached distribution
            sum += noise[i];
        }

        // Apply noise
        int i = 0;
        double priorSum = 0.0;
        for (Node child : root.children.values()) {
            double n = noise[i] / sum; // Normalize noise
            child.priorProbability = (1 - DIR_EPS) * child.priorProbability + DIR_EPS * n;
            priorSum += child.priorProbability;
            i++;
        }
        
        // Re-normalize to ensure sum is exactly 1.0
        if (priorSum > 0) {
            for (Node child : root.children.values()) child.priorProbability /= priorSum;
        }
    }
}