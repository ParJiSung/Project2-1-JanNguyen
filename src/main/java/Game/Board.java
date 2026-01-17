package Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Board {
    private final int n;
    private final Color[] cells;
    private final UnionFind uf;
    private final int redTop, redBottom, blackLeft, blackRight;
    
    // Undo/Redo support for MCTS optimization
    private static class MoveSnapshot {
        final int cellIndex;
        final Color previousColor;
        final int[] ufParent;
        final int[] ufRank;
        
        MoveSnapshot(int cellIndex, Color previousColor, UnionFind uf) {
            this.cellIndex = cellIndex;
            this.previousColor = previousColor;
            // Snapshot UnionFind state
            this.ufParent = Arrays.copyOf(uf.getParentArray(), uf.getParentArray().length);
            this.ufRank = Arrays.copyOf(uf.getRankArray(), uf.getRankArray().length);
        }
    }
    
    private final ArrayList<MoveSnapshot> moveHistory = new ArrayList<>();

    // --- 1. PUBLIC CONSTRUCTOR (Restored) ---
    public Board(int n) {
        if (n <= 0) throw new IllegalArgumentException("The board can t have less than 1 row, 1 column");
        this.n = n;
        this.cells = new Color[n * n];
        Arrays.fill(cells, Color.EMPTY);
        int unionFindSize = n * n + 4;
        this.uf = new UnionFind(unionFindSize);

        // Indices for edge nodes
        redTop = n * n;
        redBottom = n * n + 1;
        blackLeft = n * n + 2;
        blackRight = n * n + 3;
    }

    // --- 2. PRIVATE CONSTRUCTOR (For Fast Copy) ---
    private Board(int n, Color[] cells, UnionFind uf) {
        this.n = n;
        this.cells = cells;
        this.uf = uf;
        
        // Recalculate constants
        this.redTop = n * n;
        this.redBottom = n * n + 1;
        this.blackLeft = n * n + 2;
        this.blackRight = n * n + 3;
    }

    // --- 3. FAST COPY METHOD ---
    public Board fastCopy() {
        // Clone the cell array (Fast System Copy)
        Color[] newCells = new Color[this.cells.length];
        System.arraycopy(this.cells, 0, newCells, 0, this.cells.length);
        
        // Clone the UnionFind (Fast System Copy)
        // Ensure your UnionFind class has a .copy() method as discussed!
        UnionFind newUf = this.uf.copy();
        
        return new Board(this.n, newCells, newUf);
    }

    /* Get methods */
    public int getSize() { return n; }
    
    public Color getCell(int row, int column) {
        return cells[idx(row, column)];
    }

    public void getMoveRed(int row, int column, Color _ignored) {
        terminate(row, column, Color.RED);
    }

    public void getMoveBlack(int row, int column, Color _ignored) {
        terminate(row, column, Color.BLACK);
    }

    /* Helpers */
    public boolean inBounds(int row, int column) {
        return row >= 0 && column >= 0 && row < n && column < n;
    }

    public boolean isEmpty(int row, int column) {
        return inBounds(row, column) && getCell(row, column) == Color.EMPTY;
    }

    public boolean redWins() { return uf.connected(redTop, redBottom); }
    public boolean blackWins() { return uf.connected(blackLeft, blackRight); }

    public boolean isTerminal() {
        return redWins() || blackWins();
    }

    public List<int[]> legalMoves() {
        List<int[]> legalMovesList = new ArrayList<>();
        for (int row = 0; row < n; row++) {
            for (int column = 0; column < n; column++) {
                if (isEmpty(row, column)) {
                    legalMovesList.add(new int[]{row, column});
                }
            }
        }
        return legalMovesList;
    }

    private void terminate(int row, int column, Color stone) {
        if (!inBounds(row, column)) throw new IndexOutOfBoundsException();

        int cellIndex = idx(row, column);
        if (cells[cellIndex] != Color.EMPTY) throw new IllegalStateException("Cell not empty");
        
        // Save state for undo (before making the move)
        Color previousColor = cells[cellIndex];
        MoveSnapshot snapshot = new MoveSnapshot(cellIndex, previousColor, uf);
        moveHistory.add(snapshot);
        
        cells[cellIndex] = stone;
        List<int[]> list_of_neighbors = neighbors(row, column);
        
        for (var neighbor : list_of_neighbors) {
            int neighborIndex = idx(neighbor[0], neighbor[1]);
            if (cells[neighborIndex] == stone) {
                uf.union(cellIndex, neighborIndex);
            }
        }

        if (stone == Color.RED) {
            if (row == 0) uf.union(cellIndex, redTop);
            if (row == n - 1) uf.union(cellIndex, redBottom);
        } else if (stone == Color.BLACK) {
            if (column == 0) uf.union(cellIndex, blackLeft);
            if (column == n - 1) uf.union(cellIndex, blackRight);
        }
    }
    
    /**
     * Undoes the last move. Optimized for MCTS tree traversal.
     * @return true if a move was undone, false if no moves to undo
     */
    public boolean undoMove() {
        if (moveHistory.isEmpty()) return false;
        
        MoveSnapshot snapshot = moveHistory.remove(moveHistory.size() - 1);
        
        // Restore cell
        cells[snapshot.cellIndex] = snapshot.previousColor;
        
        // Restore UnionFind state
        uf.restore(snapshot.ufParent, snapshot.ufRank);
        
        return true;
    }
    
    /**
     * Gets the number of moves in history (for debugging).
     */
    public int getMoveHistorySize() {
        return moveHistory.size();
    }
    
    /**
     * Clears move history (useful when resetting or starting new game).
     */
    public void clearMoveHistory() {
        moveHistory.clear();
    }

    public List<int[]> neighbors(int row, int column) {
        int[][] neighbor_deltas = { {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}};
        List<int[]> neighbors = new ArrayList<>(6);
        for (var delta : neighbor_deltas) {
            int nr = row + delta[0];
            int nc = column + delta[1];
            if (inBounds(nr, nc)) neighbors.add(new int[]{nr, nc});
        }
        return neighbors;
    }

    private int idx(int row, int column) { return row * n + column; }

    public void clearCell(int row, int column) {
        if (inBounds(row, column)) cells[idx(row, column)] = Color.EMPTY;
    }

    public void reset() {
        Arrays.fill(cells, Color.EMPTY);
        uf.reset();
    }
}