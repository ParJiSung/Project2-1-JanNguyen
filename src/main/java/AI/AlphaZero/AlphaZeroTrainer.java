package AI.AlphaZero;

import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import AI.mcts.HexGame.Move;
import AI.mcts.Node;
import Game.Board;
import Game.Color;

public class AlphaZeroTrainer {
    private AlphaZeroNet network;
    private AlphaZeroMCTS mcts;
    private int boardSize;

    public AlphaZeroTrainer(int boardSize) {
        this.boardSize = boardSize;
        this.network = new AlphaZeroNet(boardSize);
        this.mcts = new AlphaZeroMCTS(network);
    }

    /**
     * The main method to start the training process.
     * @param numGames How many self-play games to run.
     * @param mctsIterations How many mcts iterations per move.
     */
    public void train(int numGames, int mctsIterations) {
        for (int i = 0; i < numGames; i++) {
            System.out.println("Starting Self-Play Game " + (i + 1));
            
            // Play one full game and collect data
            List<TrainingExampleData> examples = selfPlay(mctsIterations);
            
            // Train the network on this data
            // In a real scenario, you would accumulate many games before training, 
            // but for simplicity, we train after every game here.
            trainNetwork(examples);
            
            System.out.println("Game " + (i+1) + " finished. Training complete.");
        }
        
        // Save the trained model
        try {
            network.save("hex_alphazero_model.zip");
            System.out.println("Model is successfully saved!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulates one full game of Self-Play.
     */
    private List<TrainingExampleData> selfPlay(int iterations) {
        List<TrainingExampleData> gameHistory = new ArrayList<>();
        Board board = new Board(boardSize);
        Color currentPlayer = Color.RED;
        int moveCount = 0;

        while (!board.isTerminal()) {
            // Run MCTS to get the root of the search tree
            Node root = mcts.search(board, currentPlayer, iterations);

            // Extract the Policy from the root's visit counts
            // For the first 30 moves, use temperature=1 (explore), then temperature=0 (exploit)
            double temp = (moveCount < 30) ? 1.0 : 0.2; // Shortened for testing
            double[] policy = mcts.getSearchPolicy(root, temp, boardSize);

            // Store the state and the target policy
            INDArray input = BoardEncoder.encode(board, currentPlayer);
            INDArray policyTensor = Nd4j.create(policy).reshape(1, boardSize * boardSize);
            
            // We do not know the winner ("Value") yet, so we store null for now
            // We use a placeholder value (0.0) that we will overwrite later.
            gameHistory.add(new TrainingExampleData(input, policyTensor, 0.0));

            // Select a move based on the policy
            Move bestMove = selectMoveFromPolicy(policy, board);
            
            // Apply move
            if (currentPlayer == Color.RED) board.getMoveRed(bestMove.row, bestMove.col, null);
            else board.getMoveBlack(bestMove.row, bestMove.col, null);
            
            currentPlayer = (currentPlayer == Color.RED) ? Color.BLACK : Color.RED;
            moveCount++;
        }

        // The game is over. Assign the actual result (Value) to all examples
        double result = 0.0;
        if (board.redWins()) result = 1.0;     // Red Win
        else if (board.blackWins()) result = -1.0; // Black Win (Red Loss)

        // Backfill the "Value" target.
        // The value must be relative to the player who was deciding!
        // If Red won (Result=1), then for a board where Red was playing, Target=1.
        // But for a board where Black was playing, Target=-1.
        Color historyPlayer = Color.RED; // We know game started with Red
        
        List<TrainingExampleData> finalExamples = new ArrayList<>();
        for (TrainingExampleData example : gameHistory) {
            double relativeValue = (historyPlayer == Color.RED) ? result : -result;
            
            // Re-create the example with the correct value
            finalExamples.add(new TrainingExampleData(example.inputBoard, example.targetPolicy, relativeValue));
            
            // Switch player for next example
            historyPlayer = (historyPlayer == Color.RED) ? Color.BLACK : Color.RED;
        }

        return finalExamples;
    }

    /**
     * Helper to pick a move index based on the probability distribution.
     */
    private Move selectMoveFromPolicy(double[] policy, Board board) {
        // Generate a random number between 0 and 1.
        double randomNumber = Math.random();

        double sum = 0;
        int selectedIdx = -1;
        
        // Loop over all the entries in the policy array.
        for (int i = 0; i < policy.length; i++) {
            sum += policy[i]; // Cumulative sum of probabilities
            if (randomNumber <= sum) { // If the random number falls within this range
                selectedIdx = i; // Select this move
                break;
            }
        }
        
        // Fallback if something went wrong
        if (selectedIdx == -1) {
            for (int i=0; i<policy.length; i++) if (policy[i] > 0) selectedIdx = i;
        }

        int row = selectedIdx / boardSize;
        int col = selectedIdx % boardSize;
        return new Move(row, col);
    }

    /**
     * Feeds the collected examples into the neural network to update weights.
     */
    private void trainNetwork(List<TrainingExampleData> examples) {
        if (examples.isEmpty()) return; // Nothing to train on

        // Convert List of examples into one giant batch (DataSet)
        INDArray[] boardFeatures = new INDArray[examples.size()];
        INDArray[] policies = new INDArray[examples.size()];
        INDArray[] values = new INDArray[examples.size()];

        for (int i = 0; i < examples.size(); i++) {
            boardFeatures[i] = examples.get(i).inputBoard;
            policies[i] = examples.get(i).targetPolicy;
            values[i] = examples.get(i).targetValue;
        }

        // Stack them along the batch dimension (Dimension 0)
        INDArray batchBoardFeatures = Nd4j.concat(0, boardFeatures);
        INDArray batchPolicies = Nd4j.concat(0, policies);
        INDArray batchValues = Nd4j.concat(0, values);

        // Create MultiDataSet (Inputs -> [PolicyOutput, ValueOutput])
        org.nd4j.linalg.dataset.MultiDataSet dataset = new org.nd4j.linalg.dataset.MultiDataSet(
            new INDArray[]{batchBoardFeatures}, 
            new INDArray[]{batchPolicies, batchValues}
        );

        // Train the network with the dataset
        network.getModel().fit(dataset);
    }
}