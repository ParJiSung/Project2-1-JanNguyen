package AI.AlphaZero;

import Game.Board;
import Game.Color;

public class BoardEncoder {

    public static float[] encode(Board board, Color currentPlayer) {
        int size = board.getSize();
        int planeSize = size * size;
        
        // Allocate flat array: 3 planes * width * height
        float[] flatData = new float[3 * planeSize];

        int offsetRed = 0;
        int offsetBlack = planeSize;
        int offsetTurn = 2 * planeSize;

        // Fill array using standard Java loops (Extremely fast L1 cache access)
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Color cell = board.getCell(row, col);
                int idx = row * size + col;

                if (cell == Color.RED) {
                    flatData[offsetRed + idx] = 1.0f;
                } else if (cell == Color.BLACK) {
                    flatData[offsetBlack + idx] = 1.0f;
                }

                // Plane 3: Current player indicator (Fill entire plane)
                if (currentPlayer == Color.RED) {
                    flatData[offsetTurn + idx] = 1.0f;
                }
            }
        }
        
        return flatData;
    }
}