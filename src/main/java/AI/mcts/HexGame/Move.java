package AI.mcts.HexGame;

import java.util.Objects;

public class Move {
    public final int row;
    public final int col;

    // --- CACHE START ---
    // 1. Create a static cache for board sizes up to 20x20 (Adjust if needed)
    private static final Move[][] CACHE = new Move[20][20];

    // 2. Static initializer to fill the cache at startup
    static {
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 20; c++) {
                CACHE[r][c] = new Move(r, c);
            }
        }
    }

    // 3. Static Factory Method - USE THIS instead of 'new Move()'
    public static Move get(int row, int col) {
        // If within cache bounds, return cached instance
        if (row >= 0 && row < 20 && col >= 0 && col < 20) {
            return CACHE[row][col];
        }
        // Fallback for weird edge cases (rare)
        return new Move(row, col);
    }
    // --- CACHE END ---

    // 4. Make constructor PRIVATE so you are forced to use Move.get()
    private Move(int row, int col){
        this.row = row;
        this.col = col;
    }

    public String getCoordinate(){
        return "(" + row + "," + col + ")";
    }

    @Override
    public String toString(){
        return getCoordinate();
    }
    
    // Equals and HashCode are still useful, though strict reference equality (==) 
    // often works when caching is perfect. We keep them for safety.
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return row == m.row && col == m.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}