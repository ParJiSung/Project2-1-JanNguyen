// package AI.hex;
package AI.hex;

public final class HexState {
    public final int n;
    public final int[] cells;
    public final boolean plusIsRed;

    public HexState(int n, int[] cells, boolean plusIsRed) {
        this.n = n;
        this.cells = cells;
        this.plusIsRed = plusIsRed;
    }

    public HexState withMove(int action) {
        int[] next = cells.clone();
        if (next[action] != 0) throw new IllegalStateException("Illegal move");
        next[action] = +1;
        return new HexState(n, next, plusIsRed);
    }

    public HexState flippedPerspective() {
        int[] next = new int[cells.length];
        for (int i = 0; i < cells.length; i++) next[i] = -cells[i];
        return new HexState(n, next, !plusIsRed);
    }
}
