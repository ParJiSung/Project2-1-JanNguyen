package AI.mcts.Steps.SimulationStep;

import java.util.*;
import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;

public class BaseSimulation implements Simulation {
    private final Random random = new Random();

    @Override
    public int simulate(GameState start) {
        GameState state = start.copy();
        while (!state.isTerminal()) {
            List<Move> legal = state.getLegalMoves();
            if (legal.isEmpty()) break;

            Move chosen = legal.get(random.nextInt(legal.size()));
            state.doMove(chosen);
        }
        return state.getWinnerId();
    }
}