package Game;

public class BoardAdapter {
    private final Board board;
    private final int[][] matrix;

    public BoardAdapter(Board board) {
        this.board = board;
        this.matrix = new int[board.getSize()][board.getSize()];
        updateMatrix();
    }



    //Updates the 2D matrix  based on the current state of the board
    public void updateMatrix() {
        int n = board.getSize();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                Color cellColor = board.getCell(row, col);
                matrix[row][col] = colorToInt(cellColor);
            }
        }
    }

    //Updates the board within the changes in the matrix
    public void updateBoard() {
        board.reset();
        int n = board.getSize();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int v = matrix[row][col];
                if (v == 1) {
                    board.getMoveRed(row, col, Color.RED);
                } else if (v == 2) {
                    board.getMoveBlack(row, col, Color.BLACK);
                }
            }
        }
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public Board getBoard() {
        return board;
    }


     //Converts Color enum to integer representation for matrix
     //Where 0 = empty, 1 = red, 2 = blsck

    private int colorToInt(Color color) {
        switch (color) {
            case RED: return 1;
            case BLACK: return 2;
            case EMPTY:
            default: return 0;
        }
    }


    private Color intToColor(int value) {
        switch (value) {
            case 1: return Color.RED;
            case 2: return Color.BLACK;
            case 0:
            default: return Color.EMPTY;
        }
    }

    public boolean makeMove(int row, int col, Player player) {
        if (!Rules.validMove(board, row, col)) {
            return false;
        }

        // Update matrix
        matrix[row][col] = player.id;

        // Update board
        if (player == Player.RED) {
            board.getMoveRed(row, col, Color.RED);
        } else {
            board.getMoveBlack(row, col, Color.BLACK);
        }

        return true;
    }


    public void undoMove(int row, int col) {
        matrix[row][col] = 0;
        updateBoard();
    }


    public boolean isGameOver() {
        return board.isTerminal();
    }


    public Player getWinner() {
        if (board.redWins()) {
            return Player.RED;
        } else if (board.blackWins()) {
            return Player.BLACK;
        }
        return null;
    }

     //Resets both the matrix and the board
    public void reset() {
        int n = board.getSize();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                matrix[row][col] = 0;
            }
        }
        board.reset();
    }

    public Color getCellColor(int row, int col) {
        return board.getCell(row, col);
    }
}
