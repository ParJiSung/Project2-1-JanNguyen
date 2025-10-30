package UI;

import Game.BoardAdapter;
import Game.Color;
import Game.Player;
import Game.Rules;

/** Controller class to manage game logic and interactions between the BoardAdapter and BoardView 
 * @author Team 04
*/
public class GameController {
    private BoardView boardView;
    private Player currentPlayer;
    private boolean gameOver;
    private BoardAdapter adapter;
    private final Player RED = Player.RED;
    private final Player BLACK = Player.BLACK;
    private int moveCount = 0;
    /*
     * Constructor for GameController
     * @param adapter0 The BoardAdapter to interact with the game board.
     * @param boardView The BoardView to update the UI.
     */
    public GameController(BoardAdapter adapter0,BoardView boardView) {
        this.boardView = boardView;
        this.currentPlayer = RED; // can switch to BLACK to let black start
        this.gameOver = false;
        this.adapter = adapter0; // Initialize the adapter
        boardView.setController(this);
        boardView.updateTurnDisplay(currentPlayer);
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
        if (gameOver) {
            return;
        }
        // Check if pie rule is available (second player's first move)
        if (Rules.pieRuleAvailable(moveCount, currentPlayer)) {
            // Check if clicking on opponent's stone
            Color clickedCell = adapter.getCellColor(row, col);
            if (clickedCell == currentPlayer.other().stone) {
                // Swap the stone
                adapter.undoMove(row, col); // Remove opponent's stone
                adapter.makeMove(row, col, currentPlayer); // Place own stone
                
                // Update the BoardView
                boardView.update(adapter);
                
                // Move to next turn
                moveCount++;
                System.out.println(moveCount);
                currentPlayer = currentPlayer.other();
                boardView.updateTurnDisplay(currentPlayer);
                return;
            }
        }
        // Make the move on the board
        boolean ok = adapter.makeMove(row, col, currentPlayer);
        if (!ok) {
            return;
        }

        // Update the BoardView to reflect the move
        boardView.update(adapter);
        // Increment move count
        moveCount++;
        // Check for a win condition
        if (adapter.isGameOver()) {
            gameOver = true;
            // Determine winner
            String winText;
            Player winner = adapter.getWinner();
            switch (winner) {
                case RED:
                    winText = "RED wins!";
                    break;
                case BLACK:
                    winText = "BLACK wins!";
                    break;
                default:
                    winText = "It's a draw!";
            }
            System.out.println(winText);
            // Update the label to show the winner
            boardView.updateWinDisplay(winner);
            return; // Exit early, don't switch players
        }

        // No win yet, switch players
        currentPlayer = currentPlayer.other();
        boardView.updateTurnDisplay(currentPlayer); // Update the turn display
        // For debugging: print current player which how we could pop a dialog
        System.out.println("Current player: " + currentPlayer);
        
    }

    // somewhere in your UI/GameController, as an AI move method
    private void playMctsMove() {
        int n = adapter.getBoard().getSize();

        // build canonical cells: +1 = current (AI) player, -1 = opponent
        boolean plusIsRed = (getCurrentPlayer() == Player.RED);
        int[] cells = new int[n * n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                Game.Color cc = adapter.getCellColor(r, c);
                int idx = r * n + c;
                if (cc == Game.Color.EMPTY) cells[idx] = 0;
                else if (cc == Game.Color.RED)  cells[idx] = plusIsRed ? +1 : -1;
                else /* BLACK */                cells[idx] = plusIsRed ? -1 : +1;
            }
        }

        AI.hex.HexMctsAdapter game = new AI.hex.HexMctsAdapter(n);
        AI.mcts.MctsArgs args = new AI.mcts.MctsArgs(1.4, 400); // tune searches for UI speed
        AI.hex.HexState root = new AI.hex.HexState(n, cells, plusIsRed);

        AI.mcts.MCTS<AI.hex.HexState> mcts = new AI.mcts.MCTS<>(game, args);
        double[] policy = mcts.search(root);

        int best = 0;
        for (int a = 1; a < policy.length; a++) if (policy[a] > policy[best]) best = a;
        int row = best / n, col = best % n;

        // apply in your game
        adapter.makeMove(row, col, getCurrentPlayer());
        boardView.update(adapter);

        // win check & switch turn like you already do…
    }

}
