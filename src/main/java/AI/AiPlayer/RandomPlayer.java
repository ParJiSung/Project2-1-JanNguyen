package AI.AiPlayer;

import java.util.List;

import AI.mcts.HexGame.GameState;
import AI.mcts.HexGame.Move;
import Game.Board;
import Game.Player;

/**
 * Random Player class that selects moves randomly.
 * Is used to compare the performance of more advanced AI strategies.
 * 
 * @author Team 04
 */
public class RandomPlayer implements AIAgent {
    private final Player randomPlayer;

    /**
     * Constructor for RandomPlayer
     * @param randomPlayer The player this AI represents (RED or BLACK)
     */
    public RandomPlayer(Player randomPlayer) {
        this.randomPlayer = randomPlayer;
    }

    @Override
    /**
     * Randomly chooses a move for the random player.
     * 
     * @param board The current game board state
     * @param currentPlayer The player whose turn it is
     * @return A random move from the list of legal moves, or null if no moves are available
     */
    public Move getBestMove(Board board, Player currentPlayer) {
        GameState gameState = new GameState(board, currentPlayer);

        List<Move> possibleMoves = gameState.getLegalMoves();

        if (possibleMoves.isEmpty()) {
            return null;
        }

        int randomIndex = (int) (Math.random() * possibleMoves.size());
        return possibleMoves.get(randomIndex);
    }

    @Override
    /**
     * Gets the player this random agent represents.
     * @return The player (RED or BLACK)
     */
    public Player getPlayer() {
        return randomPlayer;
    }

    @Override
    /**
     * Checks if the given player is controlled by this random agent.
     * @param player The player to check
     * @return true if this random agent controls the player, false otherwise
     */
    public boolean controlsPlayer(Player player) {
        return this.randomPlayer == player;
    }
}
