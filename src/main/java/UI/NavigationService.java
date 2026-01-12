package UI;

import java.awt.Desktop;
import java.io.File;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import Game.Board;
import Game.BoardAdapter;
import Game.Player;
import AI.AiPlayer.AIAgent;
import AI.AiPlayer.MCTSPlayer;
import AI.AiPlayer.RandomPlayer;

public final class NavigationService {
    private final Stage stage;

    public NavigationService(Stage stage) {
        this.stage = stage;
    }

    // Builds the menu
    public void showMenu() {
        Parent root = MainMenu.createRoot(this);
        Scene scene = new Scene(root, 720, 480);
        attachCss(scene);
        stage.setTitle("Connections — Main Menu");
        stage.setScene(scene);
    }

    /**
     * Shows the game when no AI is involved, so a human plays against a human.
     * @param size size of the Hex board
     * @param hexSize size of the hexagons
     */
    public void showGame(int size, double hexSize) {
        Board board = new Board(size);
        BoardAdapter adapter = new BoardAdapter(board);
        BoardView boardView = new BoardView(size, hexSize);
        GameController controller = new GameController(adapter, boardView);
        boardView.setController(controller); // (controller also sets itself in ctor)

        // Create turn label
        Label turnLabel = new Label();
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        // Set the label in the view
        boardView.setTurnLabel(turnLabel);

        // Wrap BoardView in a BorderPane to add controls
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #DEB887, #D2A679, #C8A882);");
        root.setCenter(boardView);

        // Create a back button
        Button backBtn = Buttons.primary("← Back to Menu");
        backBtn.setOnAction(e -> showMenu());

        // Add button to the top
        HBox topBar = new HBox(12, backBtn, turnLabel);
        topBar.setPadding(new Insets(12));
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        Scene scene = new Scene(root, 900, 900);
        attachCss(scene);
        stage.setTitle("Hex — Game");
        stage.setScene(scene);

        // initial paint
        boardView.update(adapter);
        boardView.updateTurnDisplay(controller.getCurrentPlayer());
    }

    /**
     * Shows the game when one AI agent is involved, so a human plays against the computer.
     * @param size size of the Hex board
     * @param hexSize size of the hexagons
     * @param aiIterations number of iterations for the AI's Monte Carlo Tree Search
     */
    public void showGame(int size, double hexSize, Integer aiIterations) {
        Board board = new Board(size);
        BoardAdapter adapter = new BoardAdapter(board);
        BoardView boardView = new BoardView(size, hexSize);
        GameController controller = new GameController(adapter, boardView);
        boardView.setController(controller); // (controller also sets itself in ctor)

        // Create turn label
        Label turnLabel = new Label();
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        // Set the label in the view
        boardView.setTurnLabel(turnLabel);

        // Wrap BoardView in a BorderPane to add controls
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #DEB887, #D2A679, #C8A882);");
        root.setCenter(boardView);

        // Create a back button
        Button backBtn = Buttons.primary("← Back to Menu");
        backBtn.setOnAction(e -> {
            controller.removeAllAIAgents();
            showMenu();
        });

        // Add button to the top
        HBox topBar = new HBox(12, backBtn, turnLabel);
        topBar.setPadding(new Insets(12));
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        Scene scene = new Scene(root, 900, 900);
        attachCss(scene);
        stage.setTitle("Hex — Game");
        stage.setScene(scene);

        // initial paint
        boardView.update(adapter);
        boardView.updateTurnDisplay(controller.getCurrentPlayer());

        // Setup single AI agent for BLACK player
        MCTSPlayer mctsPlayer = new MCTSPlayer(Player.BLACK, aiIterations);
        controller.setupAIAgent(Player.BLACK, mctsPlayer);
    }

    /**
     * Shows AI vs AI testing interface.
     */
    public void showAITesting() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("AI Testing");
        dialog.setHeaderText("AI Agent Testing");
        dialog.setContentText("Choose which agents to test:");
        styleDialog(dialog);

        Button mctsVsRandomBtn = new Button("MCTS vs Random");
        Button mctsVsMctsBtn = new Button("MCTS vs MCTS");
        Button randomVsRandomBtn = new Button("Random vs Random");
        Button cancelBtn = new Button("Cancel");

        mctsVsRandomBtn.setOnAction(e -> {
            dialog.close();
            showGameAIvsAI(
                    new MCTSPlayer(Player.RED, 2000, 0.9,0.5,0.5, 0.046,0.039, Math.sqrt(2)),
                    new RandomPlayer(Player.BLACK)
            );
        });

        mctsVsMctsBtn.setOnAction(e -> {
            dialog.close();
            showGameAIvsAI(
                    new MCTSPlayer(Player.RED, 2000, 0.9,0.5,0.5, 0.046,0.039, Math.sqrt(2)),
                    new MCTSPlayer(Player.RED, 2000, 0.9,0.5,0.5, 0.046,0.039, Math.sqrt(2))
            );
        });

        cancelBtn.setOnAction(e -> dialog.close());

        VBox buttonBox = new VBox(10, mctsVsRandomBtn, mctsVsMctsBtn, randomVsRandomBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(buttonBox);
        dialog.showAndWait();
    }

    /**
     * Shows a game where two AI agents play against each other.
     */
    private void showGameAIvsAI(AIAgent redAgent, AIAgent blackAgent) {
        Board board = new Board(11);
        BoardAdapter adapter = new BoardAdapter(board);
        BoardView boardView = new BoardView(11, 55);
        GameController controller = new GameController(adapter, boardView);
        boardView.setController(controller);

        // Setup UI
        Label turnLabel = new Label();
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        boardView.setTurnLabel(turnLabel);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #DEB887, #D2A679, #C8A882);");
        root.setCenter(boardView);

        Button backBtn = Buttons.primary("← Back to Menu");
        backBtn.setOnAction(e -> {
            controller.removeAllAIAgents();
            showMenu();
        });

        HBox topBar = new HBox(12, backBtn, turnLabel);
        topBar.setPadding(new Insets(12));
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        Scene scene = new Scene(root, 900, 900);
        attachCss(scene);
        stage.setTitle("Hex – AI vs AI Testing");
        stage.setScene(scene);

        boardView.update(adapter);
        boardView.updateTurnDisplay(controller.getCurrentPlayer());

        // Setup BOTH AI agents
        controller.setupAIAgent(Player.RED, redAgent);
        controller.setupAIAgent(Player.BLACK, blackAgent);
        // The first agent will automatically start playing
    }

    //GAMBLIIIIIIIIIIIIING
    public void showSkins() {
        Parent root = new CaseOpeningView().createContent();
        Scene scene = new Scene(root, 832, 400);
        attachCss(scene);
        stage.setTitle("Case Opening");
        stage.setScene(scene);
    }

    //About button
    public void info(String title, String header, String content) {
        try {
            var url = MainMenu.class.getResource("/HEX_RULES.pdf");
            File pdf = new File(url.toURI());
            Desktop.getDesktop().open(pdf);
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "No files was found");
            styleDialog(a);
            a.showAndWait();
        }
    }

    /**
     * Shows a difficulty selection dialog for playing against the AI.
     */
    public void showDifficultySelection() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("AI Difficulty");
        dialog.setHeaderText("Select Difficulty Level");
        dialog.setContentText("Choose how strong the AI opponent should be:");
        styleDialog(dialog);

        Button easyBtn = new Button("Easy (500 iterations)");
        Button mediumBtn = new Button("Medium (1000 iterations)");
        Button hardBtn = new Button("Hard (2000 iterations)");
        Button expertBtn = new Button("Expert (5000 iterations)");
        Button cancelBtn = new Button("Cancel");

        easyBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10px;");
        mediumBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10px;");
        hardBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10px;");
        expertBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10px;");
        cancelBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10px;");

        easyBtn.setOnAction(e -> {
            dialog.close();
            showGame(11, 55, 500);
        });

        mediumBtn.setOnAction(e -> {
            dialog.close();
            showGame(11, 55, 1000);
        });

        hardBtn.setOnAction(e -> {
            dialog.close();
            showGame(11, 55, 2000);
        });

        expertBtn.setOnAction(e -> {
            dialog.close();
            showGame(11, 55, 5000);
        });

        cancelBtn.setOnAction(e -> dialog.close());

        VBox buttonBox = new VBox(10, easyBtn, mediumBtn, hardBtn, expertBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(buttonBox);
        dialog.showAndWait();
    }


    //CSS methods that helps to style our UI
    private void attachCss(Scene scene) {
        scene.getStylesheets().add(
                Objects.requireNonNull(MainMenu.class.getResource("/app.css")).toExternalForm()
        );
    }

    private void styleDialog(Alert a) {
        var dp = a.getDialogPane();
        dp.getStylesheets().add(
                Objects.requireNonNull(MainMenu.class.getResource("/app.css")).toExternalForm()
        );
        dp.getStyleClass().add("themed-dialog");
    }
}
