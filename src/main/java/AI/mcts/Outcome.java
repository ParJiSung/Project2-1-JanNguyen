package AI.mcts;

public final class Outcome {
    public final double value;
    public final boolean terminal;
    public Outcome(double value, boolean terminal) {
        this.value = value;
        this.terminal = terminal;
    }
}
