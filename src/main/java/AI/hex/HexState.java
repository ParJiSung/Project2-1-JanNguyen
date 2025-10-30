// package AI.hex;
package AI.hex;

public final class HexState {
    public final int n;
    /** Board encoding from the CURRENT player's perspective:
     *  +1 = current player's stones, -1 = opponent stones, 0 = empty. */
    public final int[] cells;
    /** In THIS encoding, do +1 stones represent RED on the physical board? */
    public final boolean plusIsRed;

    public HexState(int n, int[] cells, boolean plusIsRed) {
        this.n = n;
        this.cells = cells;
        this.plusIsRed = plusIsRed;
    }

    public HexState withMove(int action) {
        int[] next = cells.clone();
        if (next[action] != 0) throw new IllegalStateException("Illegal move");
        next[action] = +1; // current player places +1
        return new HexState(n, next, plusIsRed);
    }

    public HexState flippedPerspective() {
        int[] next = new int[cells.length];
        for (int i = 0; i < cells.length; i++) next[i] = -cells[i];
        // flip plusIsRed so that physical colors remain the same
        return new HexState(n, next, !plusIsRed);
    }
}
