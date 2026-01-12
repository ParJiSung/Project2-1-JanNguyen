package AI.mcts.Steps;

import AI.mcts.Node;

public class Backpropagation {
    /*
    This method is in charge of updating the statistics (like wins and visits) for each node and bring them up to the root. 
     */
    public void backpropagate(Node node, int winner){
        Node current = node; 
        while (current != null){
            current.visits++; // Each time this node is visited during backpropagation, the visits increase              
            if (current.playerThatMoved == winner && winner != 0){ // if the player that made the move to reach this node is the winner
                current.wins += 1.0; 
            }
            // else: if the player lost 0 is added. 
            current = current.parent; // move one level up to the parent node. 
        }
    }
}
