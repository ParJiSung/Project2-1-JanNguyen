// package AI.hex;
package AI.hex;

import java.util.concurrent.ThreadLocalRandom;

import AI.mcts.MctsGame;
import AI.mcts.Outcome;
import Game.Board;
import Game.Color;

public final class HexMctsAdapter implements MctsGame<HexState> {
    private final int n;

    public HexMctsAdapter(int n) {
        this.n = n;
    }

    @Override
    public int actionSize() {
        return n * n;
    }

    @Override
    public boolean[] getValidMoves(HexState s) {
        boolean[] valid = new boolean[n * n];
        for (int a = 0; a < valid.length; a++) {
            valid[a] = (s.cells[a] == 0);
        }
        return valid;
    }

    @Override
    public HexState getNextState(HexState s, int action, int player) {
        if (player != 1 && player != -1) {
            throw new IllegalArgumentException("player must be +1 or -1");
        }
        if (s.cells[action] != 0) {
            throw new IllegalStateException("Illegal move");
        }
        int[] next = s.cells.clone();
        if (player == 1) {
            next[action] = 1;
        } else {
            next[action] = -1;
        }
        return new HexState(s.n, next, s.plusIsRed);
    }

    @Override
    public HexState changePerspective(HexState s, int player) {
        if (player != -1) {
            throw new IllegalArgumentException("Expected player==-1");
        }
        return s.flippedPerspective();
    }

    @Override
    public Outcome valueAndTerminated(HexState s, Integer lastAction) {
        Board b = new Board(n);
        for (int idx = 0; idx < s.cells.length; idx++) {
            int v = s.cells[idx];
            if (v == 0) continue;
            int r = idx / n, c = idx % n;

            // In this state's encoding: +1 stones belong to current player.
            // If plusIsRed, +1 => RED; else +1 => BLACK.
            if (v == +1) {
                if (s.plusIsRed){
                    b.getMoveRed(r, c, Color.RED);
                }
                else {
                    b.getMoveBlack(r, c, Color.BLACK);
                }
            } else if (s.plusIsRed) {
                    b.getMoveBlack(r, c, Color.BLACK);
            } else {
                    b.getMoveRed(r, c, Color.RED);
            }
        }

        boolean red = b.redWins();
        boolean black = b.blackWins();
        if (!red && !black) {
            return new Outcome(0.0, false);
        }

        // If the current player connects, return +1; otherwise -1
        boolean currentWins = (s.plusIsRed && red) || (!s.plusIsRed && black);
        return new Outcome(currentWins ? +1.0 : -1.0, true);
    }

    @Override
    public int opponent(int player) {
        return -player;
    }

    @Override
    public double opponentValue(double v) {
        return -v;
    }

    /** Optional helper for quick random move (used in simulator below). */
    public int randomValidAction(HexState s) {
        boolean[] vm = getValidMoves(s);
        int count = 0;
        for (boolean x : vm) {
            if (x) {
                count++;
            }
        }
        int k = ThreadLocalRandom.current().nextInt(count);
        for (int a = 0; a < vm.length; a++) if (vm[a]) {
            if (k == 0) return a; k--;
        }
        throw new AssertionError("unreachable");
    }
}
