package AI.AiPlayer;

import AI.mcts.HexGame.Move;
import Game.Board;
import Game.Player;

/**
 * Interface for all AI agents that can play the game.
 */
public interface AIAgent {
    /**
     * Determines the best move for this agent.
     * @param board The current game board state
     * @param currentPlayer The player whose turn it is
     * @return The chosen move, or null if no moves are available
     */
    Move getBestMove(Board board, Player currentPlayer);
    
    /**
     * Gets the player this agent represents.
     * @return The player (RED or BLACK)
     */
    Player getPlayer();
    
    /**
     * Checks if the given player is controlled by this agent.
     * @param player The player to check
     * @return true if this agent controls the player, false otherwise
     */
    boolean controlsPlayer(Player player);
}