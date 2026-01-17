package AI.AiPlayer;

import java.io.IOException;

import AI.AlphaZero.AlphaZeroMCTS;
import AI.AlphaZero.AlphaZeroNet;
import AI.AlphaZero.MultiGpuBatcher;
import AI.mcts.HexGame.Move;
import AI.mcts.Node;
import Game.Board;
import Game.Color;

public class AlphaZeroPlayer {

    private final int boardSize;
    private final int iterations;

    private final AlphaZeroNet net;
    private final MultiGpuBatcher batcher;
    private final Thread batcherThread;
    private final AlphaZeroMCTS mcts;

    public AlphaZeroPlayer(String pathToModel, int boardSize, int iterations) {
        this.boardSize = boardSize;
        this.iterations = iterations;

        try {
            this.net = AlphaZeroNet.load(pathToModel, boardSize);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model from: " + pathToModel, e);
        }

        // Evaluation doesn't need insane batch sizes.
        int evalBatchSize = 1024;

        this.batcher = new MultiGpuBatcher(net, evalBatchSize);
        this.batcherThread = new Thread(batcher, "AZ-Batcher");
        this.batcherThread.setDaemon(true);
        this.batcherThread.start();

        this.mcts = new AlphaZeroMCTS(batcher, boardSize);

        // =========================
        // STEP 3: EVAL MODE
        // (Disable Dirichlet noise)
        // =========================
        this.mcts.setTrainingMode(false);
    }

    /**
     * Call this from your match engine to get the next move.
     */
    public Move chooseMove(Board board, Color currentPlayer) {
        Node root = mcts.search(board, currentPlayer, iterations);

        // =========================
        // STEP 4: GREEDY EVAL
        // (temperature = 0 -> argmax)
        // =========================
        double temperature = 0.0;
        double[] pi = mcts.getSearchPolicy(root, temperature);

        int bestIdx = 0;
        double best = -1.0;
        for (int i = 0; i < pi.length; i++) {
            if (pi[i] > best) {
                best = pi[i];
                bestIdx = i;
            }
        }

        int row = bestIdx / boardSize;
        int col = bestIdx % boardSize;
        return Move.get(row, col);
    }

    public void shutdown() {
        batcher.stop();
    }
}