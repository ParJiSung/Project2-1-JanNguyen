package UI;

import Game.BoardAdapter;
import Game.Color;
import Game.Player;
import Game.Rules;
import AI.AiPlayer.AIAgent;
import AI.mcts.HexGame.Move;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.HashMap;
import java.util.Map;

/** Controller class to manage game logic and interactions between the BoardAdapter and BoardView 
 * @author Team 04
*/
public class GameController {
    private final BoardView boardView;
    private Player currentPlayer;
    private boolean gameOver;
    private final BoardAdapter adapter;
    private int moveCount = 0;
    
    // Map with AI player(s) (can be more than one when testing performance of agents)
    private Map<Player, AIAgent> aiAgents = new HashMap<>();
    private boolean aiThinking = false;

    /**
     * Constructor for GameController
     * @param adapter0 The BoardAdapter to interact with the game board.
     * @param boardView The BoardView to update the UI.
     */
    public GameController(BoardAdapter adapter0, BoardView boardView) {
        this.boardView = boardView;
        this.currentPlayer = Player.RED; // can switch to Player.BLACK to let black start
        this.gameOver = false;
        this.adapter = adapter0; // Initialize the adapter
        this.aiAgents = new HashMap<>();
        boardView.setController(this);
        boardView.updateTurnDisplay(currentPlayer);
    }

    /**
     * Sets up an AI agent for a specific player.
     * @param player The player this AI will control (RED or BLACK).
     * @param agent The AI agent to use.
     */
    public void setupAIAgent(Player player, AIAgent agent) {
        aiAgents.put(player, agent);
        System.out.println("AI Agent set up for: " + player);
        
        // If it's this player's turn, make the AI move
        if (aiAgents.get(currentPlayer) != null && !gameOver) {
            makeAIMove();
        }
    }

    /**
     * Removes the AI agent for a specific player.
     * @param player The player whose AI should be removed
     */
    public void removeAIAgent(Player player) {
        aiAgents.remove(player);
        System.out.println("AI Agent removed for: " + player);
    }

    /**
     * Removes all AI agents from the game.
     */
    public void removeAllAIAgents() {
        aiAgents.clear();
        this.aiThinking = false;
        System.out.println("All AI Agents removed");
    }

    /**
     * Checks if any AI agents are active.
     */
    public boolean hasAnyAIAgent() {
        return !aiAgents.isEmpty();
    }

    /**
     * Checks if a specific player is controlled by an AI.
     */
    public boolean isAIControlled(Player player) {
        return aiAgents.containsKey(player);
    }

    /**
     * Gets the AI agent for a specific player, if one exists.
     */
    public AIAgent getAIAgent(Player player) {
        return aiAgents.get(player);
    }

    /**
     * Returns the current player whose turn it is to make a move.
     * 
     * @return the current player (either RED or BLACK)
    */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /** Handle a cell click at the given row, col from the BoardView. 
     * @param row The row of the clicked cell.
     * @param col The column of the clicked cell.
    */
    public void handleCellClick(int row, int col) {
        if (gameOver || aiThinking) {
            return;
        }
        
        // Block clicks if current player is AI-controlled
        if(isAIControlled(currentPlayer)){
            return;
        }
        
        // Check if pie rule is available (second player's first move)
        if (Rules.pieRuleAvailable(moveCount, currentPlayer)) {
            // Check if clicking on opponent's stone
            Color clickedCell = adapter.getCellColor(row, col);
            if (clickedCell == currentPlayer.other().stone) {
                adapter.undoMove(row, col); 
                adapter.makeMove(row, col, currentPlayer);
                boardView.update(adapter);
 
                moveCount++;
                System.out.println(moveCount);
                currentPlayer = currentPlayer.other();
                boardView.updateTurnDisplay(currentPlayer);

                return;
            }
        }
        
        boolean ok = adapter.makeMove(row, col, currentPlayer);
        if (!ok) {
            return;
        }
        // Update the BoardView to reflect the move
        boardView.update(adapter);
        moveCount++;

        if (adapter.isGameOver()) {
            gameOver = true;
            Player winner = adapter.getWinner();
            System.out.println("Player " + winner + " wins!");
            boardView.updateWinDisplay(winner);
            return;
        }

        // No win yet, switch players
        currentPlayer = currentPlayer.other();
        boardView.updateTurnDisplay(currentPlayer);
        System.out.println("Current player: " + currentPlayer);
        
        // Check if the new current player is AI-controlled
        if (isAIControlled(currentPlayer)) {
            makeAIMove();
        }
    }

    /**
     * Makes an AI move on a background thread to avoid blocking the UI.
     */
    private void makeAIMove() {
        if (aiThinking || gameOver) {
            return;
        }

        AIAgent currentAIAgent = aiAgents.get(currentPlayer);
        if (currentAIAgent == null) {
            return; // No AI agent for current player
        }

        aiThinking = true;
        
        // Run AI move calculation on a background thread
        Task<Move> aiTask = new Task<Move>() {
            @Override
            protected Move call() throws Exception {
                // Get the best move from the AI
                return currentAIAgent.getBestMove(adapter.getBoard(), currentPlayer);
            }
        };

        aiTask.setOnSucceeded(event -> {
            Move bestMove = aiTask.getValue();
            if (bestMove != null) {
                Platform.runLater(() -> {
                    handleAIMove(bestMove.row, bestMove.col);
                });
            }
            aiThinking = false;
        });

        aiTask.setOnFailed(event -> {
            System.out.println("AI move calculation failed: " + aiTask.getException());
            aiThinking = false;
        });

        Thread aiThread = new Thread(aiTask);
        aiThread.setDaemon(true);
        aiThread.start();
    }

    /**
     * Handles the actual placement of the AI's move on the board.
     * @param row The row of the move
     * @param col The column of the move
     */
    private void handleAIMove(int row, int col) {
        System.out.println("AI (" + currentPlayer + ") makes move at: (" + row + ", " + col + ")");
        
        // Make the move
        boolean ok = adapter.makeMove(row, col, currentPlayer);
        if (!ok) {
            System.out.println("AI move failed!");
            aiThinking = false;
            return;
        }

        // Update the BoardView
        boardView.update(adapter);
        moveCount++;

        // Check for a win condition
        if (adapter.isGameOver()) {
            gameOver = true;
            Player winner = adapter.getWinner();
            System.out.println("Player " + winner + " wins!");
            boardView.updateWinDisplay(winner);
            return;
        }

        // Switch to the other player
        currentPlayer = currentPlayer.other();
        boardView.updateTurnDisplay(currentPlayer);


        // // If the next player is also AI, continue
        // if (isAIControlled(currentPlayer)) {
        //     // Add a small delay to make the game more watchable
        //     Timer timer = new Timer(200, e -> makeAIMove());
        //     timer.setRepeats(false);
        //     timer.start();
        // }
    }

}