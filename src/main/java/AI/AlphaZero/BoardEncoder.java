package AI.AlphaZero;

import Game.Board;
import Game.Color;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class BoardEncoder {
    /**
     * Converts the Hex Board into a format that the neural network understands. (A 3 plane format)
     * Plane 0: Red Stones (1 if Red, 0 otherwise)
     * Plane 1: Black Stones (1 if Black, 0 otherwise)
     * Plane 2: Current Player (1 if Red to play, 0 if Black
    */
    public static INDArray encode(Board board, Color currentPlayer) {
        int size = board.getSize();

        // Shape: [BatchSize, Channels, Height, Width] -> [1, 3, size, size]
        // BatchSize is 1 since we are encoding a single board state.
        // Channels is 3 for the three planes described above.
        // Height and Width are both equal to the board size.
        INDArray convertedBoard = Nd4j.zeros(1, 3, size, size);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Color cell = board.getCell(row, col); // Get the color of the cell at (row, col)

                if (cell == Color.RED) {
                    // Batch is 0 since we only encode 1 board state, and plane 0 indicates Red stones.
                    convertedBoard.putScalar(0, 0, row, col, 1.0);
                } else if (cell == Color.BLACK) {
                    // Plane 1 indicates Black stones.
                    convertedBoard.putScalar(0, 1, row, col, 1.0);
                }
                
                // Plane 2 indicates whose turn it is
                // If it is RED's turn, fill the whole plane with 1s.
                double turnValue = (currentPlayer == Color.RED) ? 1.0 : 0.0;
                convertedBoard.putScalar(0, 2, row, col, turnValue);
            }
        }
        return convertedBoard;
    }
}
