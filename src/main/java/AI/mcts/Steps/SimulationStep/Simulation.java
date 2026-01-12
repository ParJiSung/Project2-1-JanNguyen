package AI.mcts.Steps.SimulationStep;

import AI.mcts.HexGame.GameState;

public interface Simulation {
    int simulate(GameState start);
}

