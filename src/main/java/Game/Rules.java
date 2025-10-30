package Game;

public final class Rules {
    private Rules() {}

    /* Valid cell: inside bounds. (All n×n cells are playable in Hex.) */
    public static boolean validCell(Board b, int row, int col) {
        return b.inBounds(row, col);
    }

    /* Valid move: inside bounds and EMPTY. */
    public static boolean validMove(Board b, int row, int col) {
        return validCell(b, row, col) && b.isEmpty(row, col);
    }

    /* Pie rule hook if you want it later. */
    public static boolean pieRuleAvailable(int plyCount, Player toMove) {
        return plyCount <= 1;
    }
}

