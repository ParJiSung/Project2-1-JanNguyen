package AI.mcts;
import java.util.*;

import AI.mcts.HexGame.Move;
public final class Node { // this is not a final version. it's just a skeleton to compile and try the selection class
    // fields 
    public Move move; // the move that led to this node. null for the root node
    public Node parent; // parent node 
    public Map<Move, Node> children = new HashMap<>(); // map of all the children of this node
    public int visits = 0; // how many times has this node been visited 
    public double wins = 0.0; // how many times did it win
    public int playerThatMoved; //Player 1 or player 2. 
    public double heuristicBias =0.0; // Default bias
    public double priorProbability = 0.0; // Prior probability from neural network. 

    // constructor 
    public Node(Move move, Node parent, int playerThatMoved){
        this.move = move; 
        this.parent = parent; 
        this.playerThatMoved = playerThatMoved;
    }

    // Constructor with prior probability, for use in combination with a neural network.
    public Node(Move move, Node parent, int playerThatMoved, double priorProbability){
        this.move = move; 
        this.parent = parent; 
        this.playerThatMoved = playerThatMoved;
        this.priorProbability = priorProbability;
    }
}
