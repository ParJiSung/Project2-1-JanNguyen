package UI;

import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import Game.BoardAdapter;
import Game.Color;
import Game.Player;

/**
 * Visual representation of a hexagonal game board for the Hex board game.
 * This class extends JavaFX Pane and manages the rendering of a board composed
 * of hexagonal cells arranged in a diamond pattern. It handles user interactions,
 * displays cell colors, and shows game state information including turn indicators
 * and win notifications.
 * 
 * <p>The board features:
 * <ul>
 *   <li>Hexagonal cells arranged in rows and columns</li>
 *   <li>Colored borders indicating player sides (RED: top/bottom, BLACK: left/right)</li>
 *   <li>Interactive cells that respond to mouse clicks</li>
 *   <li>Dynamic color updates based on game state</li>
 * </ul>
 */
public class BoardView extends Pane{
    private Label statusLabel = new Label(); // Label to show current player's turn
    private final int boardSize; // Number of hexagons along one edge of the board
    private final double hexSize; // Size of each hexagon
    private final Polygon[][] hexCells; // 2D array to hold hexagon shapes
    private GameController controller; // reference for click events.
    private Label turnLabel; // Label to show whose turn it is

    /**
     * Constructs a new BoardView with the specified dimensions.
     * Initializes the hexagon array and draws an empty board with borders.
     * 
     * @param boardSize the number of hexagons along one edge of the board (e.g., 11 for an 11x11 board)
     * @param hexSize the diameter of each hexagon in pixels
    */
    public BoardView(int boardSize, double hexSize) {
        this.boardSize = boardSize;
        this.hexSize = hexSize;
        this.hexCells = new Polygon[boardSize][boardSize];
        drawEmptyBoard(); // Draw the initial empty board
    }

    /**
     * Sets the game controller that will handle cell click events.
     * The controller is notified when a user clicks on any hexagonal cell.
     * 
     * @param controller the GameController instance to handle user interactions
    */
    public void setController(GameController controller) {
        this.controller = controller;
    }

    /**
     * Sets the label used to display turn information and game results.
     * This label will be updated to show whose turn it is and who won the game.
     * 
     * @param turnLabel the Label component to be updated with game state information
    */
    public void setTurnLabel(Label turnLabel) {
        this.turnLabel = turnLabel;
    }

    /**
     * Updates the visual representation of the board based on the current game state.
     * Iterates through all cells and sets their fill colors according to the adapter's
     * representation of the board state.
     * 
     * @param adapter the BoardAdapter providing access to the current cell colors
    */
    public void update(BoardAdapter adapter){
        for(int row = 0; row < boardSize; row++){
            for(int col = 0; col < boardSize; col++){
                Color cellColor = adapter.getCellColor(row, col);
                hexCells[row][col].setFill(paintForColor(cellColor));
            }
        }
    }

    /**
     * Updates the turn display label to indicate which player's turn it is.
     * Sets the text and styling based on the current player's color.
     * RED player is displayed in red (#ef4444), BLACK player in black.
     * 
     * @param currentPlayer the player whose turn it currently is (RED or BLACK)
    */
    public void updateTurnDisplay(Player currentPlayer) {
        if (turnLabel != null) {
            String playerName = (currentPlayer == Player.RED) ? "RED" : "BLACK";
            String color = (currentPlayer == Player.RED) ? "#ef4444" : "#000000ff";
            turnLabel.setText("Player " + playerName + " is in turn");
            turnLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
    }

    /**
     * Updates the label to display the game winner.
     * Sets the text and styling to indicate which player has won the game.
     * Uses larger font size and color coding to emphasize the victory announcement.
     * 
     * @param winner the player who won the game (RED or BLACK)
    */
    public void updateWinDisplay(Player winner) {
        if (turnLabel != null) {
            String playerName = (winner == Player.RED) ? "RED" : "BLACK";
            String color = (winner == Player.RED) ? "#ef4444" : "#1f2937";
            turnLabel.setText("Player " + playerName + " has won!");
            turnLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 18px;");
        }
    }
    // Helper methods
    /**
     * Draws the initial empty board with all hexagonal cells and colored borders.
     * Clears any existing children from the pane, calculates hexagon positions,
     * creates hexagon polygons, attaches click handlers, and draws the four
     * colored borders (RED for top/bottom, BLACK for left/right).
     * 
     * <p>The hexagons are arranged in a parallelogram pattern with appropriate
     * spacing and padding. Each hexagon is clickable and will notify the controller
     * when clicked.
    */
    private void drawEmptyBoard(){
        getChildren().clear();
        double radius = hexSize / 2;
        double width = hexSize;
        double height = Math.sqrt(3) * radius; // Height of one hexagon
        double padding = 2*radius; // Padding from the edges

        for(int row = 0; row < boardSize; row++){
            for(int col = 0; col < boardSize; col++){
                double x = padding + col * width * Math.sqrt(3)/2 + row * (width/2)*Math.sqrt(3)/2;
                double y = padding + row * height*Math.sqrt(3)/2;

                Polygon hex = createHexagon(x, y, radius); // Create hexagon centered at (x, y)
                hexCells[row][col] = hex;

                int finalRow = row;
                int finalCol = col;
                hex.setOnMouseClicked((MouseEvent event) -> {
                    if(controller != null){
                        controller.handleCellClick(finalRow, finalCol);

                    }
                });

                getChildren().add(hex);
            }
        }
        drawBoardBorders(padding, radius, width, height);
    }

    /**
     * Creates a single hexagon polygon centered at the specified coordinates.
     * The hexagon is oriented with pointy-top (vertices at top and bottom).
     * Each hexagon has a black stroke outline and is initially filled with
     * the EMPTY color (light gray).
     * 
     * @param centerX the x-coordinate of the hexagon's center
     * @param centerY the y-coordinate of the hexagon's center
     * @param radius the radius (distance from center to vertex) of the hexagon
     * @return a Polygon object representing the hexagon
    */
    private Polygon createHexagon(double centerX, double centerY, double radius){
        Polygon hex = new Polygon();
        for(int i = 0; i < 6; i++){
            double angle = Math.toRadians(60 * i - 30); // Pointy-top hexagon
            double xPos = centerX + radius * Math.cos(angle);
            double yPos = centerY + radius * Math.sin(angle);
            hex.getPoints().addAll(xPos, yPos);
        }
        hex.setStroke(Paint.valueOf("BLACK"));
        hex.setFill(paintForColor(Color.EMPTY));
        return hex;
    }

    /**
     * Converts a game Color enum value to a JavaFX Paint object.
     * Maps game colors to their visual representations:
     * RED becomes red, BLACK becomes black, and EMPTY becomes light gray.
     * 
     * @param color the game Color to convert
     * @return the corresponding Paint object for rendering
    */
    private Paint paintForColor(Color color){
        switch(color){
            case RED:
                return Paint.valueOf("RED");
            case BLACK:
                return Paint.valueOf("BLACK");
            default:
                return Paint.valueOf("LIGHTGRAY");
        }
    }

    /**
     * Draws the four colored borders around the game board.
     * The borders indicate which sides each player must connect:
     * <ul>
     *   <li>Top border: RED (horizontal)</li>
     *   <li>Bottom border: RED (horizontal)</li>
     *   <li>Left border: BLACK (diagonal)</li>
     *   <li>Right border: BLACK (diagonal)</li>
     * </ul>
     * 
     * <p>Each border is drawn as a polyline that follows the outer edges
     * of the hexagons along that side of the board. The borders have a
     * stroke width of 3 pixels for clear visibility.
     * 
     * @param padding the padding distance from the pane edges
     * @param radius the radius of each hexagon
     * @param width the width of each hexagon
     * @param height the height of each hexagon
    */
    private void drawBoardBorders(double padding, double radius, double width, double height) {
        // Top border (horizontal edge at the top)
        Polyline topBorder = new Polyline();
        for (int col = 0; col < boardSize; col++) {
            double x = padding + col * width * Math.sqrt(3) / 2;
            double y = padding;
            
            // Add the top-left vertex of each hexagon along the top edge
            double angle1 = Math.toRadians(60 * 4 - 30); // 210 degrees
            double xPos1 = x + radius * Math.cos(angle1);
            double yPos1 = y + radius * Math.sin(angle1);
            topBorder.getPoints().addAll(xPos1, yPos1);
            
            // Add the top-right vertex
            double angle2 = Math.toRadians(60 * 5 - 30); // 270 degrees
            double xPos2 = x + radius * Math.cos(angle2);
            double yPos2 = y + radius * Math.sin(angle2);
            topBorder.getPoints().addAll(xPos2, yPos2);

            if(col == boardSize - 1) {
                // Add the last top-right vertex of the last hexagon
                double angle3 = Math.toRadians(60 * 0 - 30); // -30 degrees (330)
                double xPos3 = xPos2 + radius * Math.cos(angle3);
                double yPos3 = y + radius * Math.sin(angle3);
                topBorder.getPoints().addAll(xPos3, yPos3);
            }
        }
        topBorder.setStroke(Paint.valueOf("RED"));
        topBorder.setStrokeWidth(3);
        getChildren().add(topBorder);
        
        // Left border (diagonal edge on the left)
        Polyline leftBorder = new Polyline();

        // Start with the top-left vertex of the first hexagon (90 degrees)
        double x0 = padding;
        double y0 = padding;
        double angle0 = Math.toRadians(60 * 4 - 30); // 210 degrees
        double xPos0 = x0 + radius*Math.cos(angle0);
        double yPos0 = y0 + radius*Math.sin(angle0);
        leftBorder.getPoints().addAll(xPos0, yPos0);

        // Now add the left vertex (150 degrees) for each row
        for (int row = 0; row < boardSize; row++) {
            double x = padding + row * (width / 2) * Math.sqrt(3) / 2;
            double y = padding + row * height * Math.sqrt(3) / 2;
            
            // Add the left vertex of each hexagon along the left edge
            double angle1 = Math.toRadians(60 * 3 - 30); // 150 degrees
            double xPos1 = x + radius * Math.cos(angle1);
            double yPos1 = y + radius * Math.sin(angle1);
            leftBorder.getPoints().addAll(xPos1, yPos1);
            
            // Add the bottom-left vertex
            double angle2 = Math.toRadians(60 * 2 - 30); // 90 degrees
            double xPos2 = x + radius * Math.cos(angle2);
            double yPos2 = y + radius * Math.sin(angle2);
            leftBorder.getPoints().addAll(xPos2, yPos2);
        }

        leftBorder.setStroke(Paint.valueOf("BLACK"));
        leftBorder.setStrokeWidth(3);
        getChildren().add(leftBorder);
        
        // Bottom border (horizontal edge at the bottom)
        Polyline bottomBorder = new Polyline();
        for (int col = boardSize - 1; col >= 0; col--) {
            int row = boardSize - 1;
            double x = padding + col * width * Math.sqrt(3) / 2 + row * (width / 2) * Math.sqrt(3) / 2;
            double y = padding + row * height * Math.sqrt(3) / 2;
            
            // Add the bottom-right vertex
            double angle1 = Math.toRadians(60 * 1 - 30); // 30 degrees
            double xPos1 = x + radius * Math.cos(angle1);
            double yPos1 = y + radius * Math.sin(angle1);
            bottomBorder.getPoints().addAll(xPos1, yPos1);
            
            // Add the bottom-left vertex
            double angle2 = Math.toRadians(60 * 2 - 30); // 90 degrees
            double xPos2 = x + radius * Math.cos(angle2);
            double yPos2 = y + radius * Math.sin(angle2);
            bottomBorder.getPoints().addAll(xPos2, yPos2);
        }
        bottomBorder.setStroke(Paint.valueOf("RED"));
        bottomBorder.setStrokeWidth(3);
        getChildren().add(bottomBorder);
        
        // Right border (diagonal edge on the right)
        Polyline rightBorder = new Polyline();

        // Now add vertices for each row going down
        for (int row = 0; row < boardSize; row++) {
            int col = boardSize - 1;
            double x = padding + col * width * Math.sqrt(3) / 2 + row * (width / 2) * Math.sqrt(3) / 2;
            double y = padding + row * height * Math.sqrt(3) / 2;
            
            // Add the right vertex
            double angle1 = Math.toRadians(60 * 0 - 30); // -30 degrees (330)
            double xPos1 = x + radius * Math.cos(angle1);
            double yPos1 = y + radius * Math.sin(angle1);
            rightBorder.getPoints().addAll(xPos1, yPos1);
            
            // Add the bottom-right vertex
            double angle2 = Math.toRadians(60 * 1 - 30); // 30 degrees
            double xPos2 = x + radius * Math.cos(angle2);
            double yPos2 = y + radius * Math.sin(angle2);
            rightBorder.getPoints().addAll(xPos2, yPos2);
        }

        rightBorder.setStroke(Paint.valueOf("BLACK"));
        rightBorder.setStrokeWidth(3);
        getChildren().add(rightBorder);
    }
   
}
