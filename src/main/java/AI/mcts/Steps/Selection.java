package AI.mcts.Steps;

import AI.mcts.Node;
import AI.mcts.HexGame.GameState;

public class Selection{
    /*
    Selection Class: 
    This class implements the selection phase of the MCTS 
    In MCTS, each node represents a game state. During the selection phase, we traverse the tree from the root to find a node that is:
    - not fully expanded (still has unexplored moves), or
    - a terminal node (game over)
    
    Selection uses the UCT formula (The one that Jan explained in the first meeting):
    UCT = (w_i / n_i) + C * sqrt( ln(N) / n_i )
    Where:
    - w_i: number of wins for the child
    - n_i: number of visits for the child
    - N: number of visits for the parent node
    - C: exploration constant (balances exploration vs exploitation)
    */

    // Exploration constant (C) used in UCT formula.
    // sqrt(2) is commonly used to balance exploration and exploitation.
    private final double c;
    
    public Selection(double c) {
        this.c = c;
    }
    
    /*
    Main Selection Method
    Goes through the tree from the given node until we reach a node that:
    - is not fully expanded (has untried moves)
    - or is terminal (end of game) -> as explained previously 
     */
    public Node select(Node node, GameState state){
        while (!state.isTerminal() && isFullyExpanded(node, state)) { // while the game is not over and the node is fully expanded
            node = bestChild(node); // choose the child that gives the best UCT. See the method below
            state.doMove(node.move); // apply the move of the selected child to the game state
            }

        return node; // return the nodes where the expansion will happen

    }

    private boolean isFullyExpanded(Node node, GameState state) {
        int legal = state.getLegalMoves().size();
        return !state.isTerminal() && node.children.size() >= legal;
    }

    private Node bestChild(Node node) {
        Node best = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (Node child : node.children.values()) {
            double uctValue;

            if (child.visits == 0) {
                uctValue = Double.POSITIVE_INFINITY;
            } else {
                double eps = 1e-9;
                double winRate = child.wins / (child.visits + eps);
                double explore = c * Math.sqrt(Math.log(node.visits + 1.0) / (child.visits + eps));
                double bias = child.heuristicBias;

                uctValue = winRate + explore + bias;
            }

            if (uctValue > bestValue) {
                bestValue = uctValue;
                best = child;
            }
        }
        return best;
    }
}

