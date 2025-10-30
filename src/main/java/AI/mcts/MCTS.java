package AI.mcts;

import java.util.Arrays;

public final class MCTS<S> {
    private final MctsGame<S> game;
    private final MctsArgs args;

    public MCTS(MctsGame<S> game, MctsArgs args) {
        this.game = game;
        this.args = args;
    }

    public double[] search (S root_state) {
        Node<S> root = new Node<>(game, args, root_state, null, null);

        for (int search = 0; search < args.numSearches; search++) {
            Node<S> node = root;

            while (node.isFullyExpanded()){
                node = node.select();
            }

            Outcome outcome = game.valueAndTerminated(node.state, node.action_taken);
            double value = game.opponentValue(outcome.value);

            if (!outcome.terminal) {
                node = node.expand();
                value = node.simulate();
            }

            node.backpropagate(value);
        }

        double[] action_probs = new double[game.actionSize()];
        for (Node<S> child : root.children) {
            action_probs[child.action_taken] = child.visit_count;
        }
        normalizeInPlace(action_probs);
        return action_probs;
    }

    private void normalizeInPlace(double[] x) {
        double sum = Arrays.stream(x).sum();
        if (sum <= 0) {
            return;
        }
        for (int i = 0; i < x.length; i++) {
            x[i] /= sum;
        }
    }
}
