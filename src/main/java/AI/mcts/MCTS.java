package AI.mcts;

import AI.mcts.HexGame.GameState;
import AI.mcts.Steps.Backpropagation;
import AI.mcts.Steps.Expansion;
import AI.mcts.Steps.Selection;
import AI.mcts.Steps.SimulationStep.*;
import AI.mcts.Optimazation.*;

public final class MCTS {
    private final Selection selection;
    private final Expansion expansion;
    private final Backpropagation backprop;
    private final Simulation simulation;

    private final int iterations;

    // added constructor for pruner
    public MCTS(int iterations, Selection selection, Expansion expansion, Simulation simulation) {
        this.iterations = iterations;
        this.selection = selection;
        this.expansion = expansion;
        this.backprop = new Backpropagation();
        this.simulation = simulation;
    }

    public Node search(Node root, GameState rootState) {
        for (int i = 0; i < iterations; i++) {
            GameState state = rootState.copy();
            Node leaf = selection.select(root, state);
            Node child = expansion.expand(leaf, state);
            if (child != leaf) state.doMove(child.move);
            int winner = state.isTerminal() ? state.getWinnerId()
                    : simulation.simulate(state);
            backprop.backpropagate(child, winner);
        }
        return bestChildByVisits(root);
    }

    private Node bestChildByVisits(Node root) {
        Node best = null; int bestV = -1;
        for (Node child : root.children.values()) {
            if (child.visits > bestV) {
                bestV = child.visits; best = child;
            }
        }
        return best;
    }

    public MovePruner getPruner() {
        return expansion.getPruner();
    }
}


