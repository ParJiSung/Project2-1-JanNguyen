package AI.mcts;

public interface MctsGame<S>{
    int actionSize();
    boolean[] getValidMoves(S state);
    S getNextState(S state, int action, int player);
    S changePerspective(S state, int player);
    Outcome valueAndTerminated(S state, Integer player);
    int opponent(int player);
    double opponentValue(double v);
}
