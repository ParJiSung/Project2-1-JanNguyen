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
import javafx.stage.Stage;

import Game.Board;
import Game.BoardAdapter;

public final class NavigationService {
    private final Stage stage;

    public NavigationService(Stage stage) {
        this.stage = stage;
    }

    //Builds the menu
    public void showMenu() {
        Parent root = MainMenu.createRoot(this);
        Scene scene = new Scene(root, 720, 480);
        attachCss(scene);
        stage.setTitle("Connections — Main Menu");
        stage.setScene(scene);
    }

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
