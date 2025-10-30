package AI.mcts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Node<S>  {
    private final MctsGame<S> game;
    private final MctsArgs args;
    public final S state;
    public final Node<S> parent;
    public final Integer action_taken;
    public final List<Node<S>> children = new ArrayList<>();
    private final boolean[] expandable_moves;
    public int visit_count = 0;
    public double value_sum = 0.0;

    public Node(MctsGame<S> game, MctsArgs args, S state, Node<S> parent, Integer action_taken) {
        this.game = game;
        this.args = args;
        this.state = state;
        this.parent = parent;
        this.action_taken = action_taken;
        this.expandable_moves = game.getValidMoves(state);
    }

    public boolean isFullyExpanded() {
        boolean any_expandable = false;
        for (boolean can_expand : expandable_moves) {
            if (can_expand) {
                any_expandable = true;
            }
        }
        return !any_expandable && !children.isEmpty();
    }

    public Node<S> select(){
        Node<S> best_child = null;
        double best_ucb = Double.NEGATIVE_INFINITY;
        for (Node<S> child : children) {
            double ucb = getUcb(child);
            if (ucb > best_ucb) {
                best_ucb = ucb;
                best_child = child;
            }
        }
        return best_child;
    }

    private double getUcb(Node<S> child) {
        double mean = child.value_sum / Math.max(1, child.visit_count);
        double qValue = 1.0 - ((mean + 1.0) / 2.0);
        double expl = args.c * Math.sqrt(Math.log(Math.max(1, this.visit_count)) / Math.max(1, child.visit_count));
        return qValue + expl;
    }

    public Node<S> expand() {
        int action = pickRandomExpandable();
        expandable_moves[action] = false;
        S child_state = game.getNextState(state, action, 1);
        child_state = game.changePerspective(child_state, -1);
        Node<S> child = new Node<>(game, args, child_state, this, action);
        children.add(child);
        return child;
    }

    public double simulate() {
        Outcome out = game.valueAndTerminated(state, action_taken);
        double value = game.opponentValue(out.value);

        if (out.terminal) {
            return value;
        }

        S rollout_state = stateCopy(state);
        int rollout_player = 1;
        while(true) {
            boolean[] valid_moves = game.getValidMoves(rollout_state);
            int action = pickRandomTrue(valid_moves);
            rollout_state = game.getNextState(rollout_state, action, rollout_player);
            Outcome o2 = game.valueAndTerminated(rollout_state, action);
            if (o2.terminal) {
                double v;
                if (rollout_player == -1) {
                    v = o2.value;
                } else {
                    v = game.opponentValue(o2.value);
                }
                return v;
            }
            rollout_player = game.opponent(rollout_player);
        }
    }

    public void backpropagate(double value){
        this.value_sum += value;
        this.visit_count += 1;

        double flipped_value = game.opponentValue(value);
        if (parent != null) {
            parent.backpropagate(flipped_value);
        }
    }

    private int pickRandomExpandable() {
        int count = 0;
        for (int i = 0; i < expandable_moves.length; i++) if (expandable_moves[i]) count++;
        if (count == 0) throw new IllegalStateException("No expandable moves left");

        int k = ThreadLocalRandom.current().nextInt(count);
        for (int i = 0; i < expandable_moves.length; i++) {
            if (expandable_moves[i]) {
                if (k == 0) return i;
                k--;
            }
        }
        throw new AssertionError("unreachable");
    }

    private int pickRandomTrue(boolean[] bits) {
        int count = 0;
        for (boolean b : bits) if (b) count++;
        int k = ThreadLocalRandom.current().nextInt(count);
        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                if (k == 0) {
                    return i;
                }
                k--;
            }
        }
        throw new AssertionError("unreachable");
    }

    private S stateCopy(S s) {
        return s;
    }
}
