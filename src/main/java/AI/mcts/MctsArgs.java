package AI.mcts;

public final class MctsArgs {
    public final double c;
    public final int numSearches;

    public MctsArgs(double c, int numSearches) {
        this.c = c;
        this.numSearches = numSearches;
    }
}