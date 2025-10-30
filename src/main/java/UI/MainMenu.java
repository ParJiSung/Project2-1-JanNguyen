package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MainMenu extends Application {

    @Override
    public void start(Stage primaryStage) {
        var nav = new NavigationService(primaryStage);
        nav.showMenu();
        primaryStage.show();
    }

    //Builds the main Menu Window
    public static Parent createRoot(NavigationService nav) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #111827);");

        Text title = new Text("Connections");
        title.setFill(javafx.scene.paint.Color.WHITE);
        title.setFont(Font.font("Inter", FontWeight.EXTRA_BOLD, 42));

        VBox header = new VBox(6, title);
        header.setAlignment(Pos.CENTER_LEFT);
        Button playBtn  = new Button("PLAY");  playBtn.getStyleClass().add("btn-primary");
        //Button skinsBtn = new Button("GAMBLING"); skinsBtn.getStyleClass().add("btn-secondary");
        Button aboutBtn = new Button("ABOUT"); aboutBtn.getStyleClass().add("btn-ghost");

        playBtn.setOnAction(e -> nav.showGame(11, 55));
        //skinsBtn.setOnAction(e -> nav.showSkins());
        aboutBtn.setOnAction(e -> nav.info("About Connections", "About the Game","Put the link to the game's rules here."));

        VBox buttons = new VBox(12, playBtn, aboutBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        HBox footer = new HBox(12, spacer());
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(24, 0, 0, 0));

        VBox content = new VBox(8, header, buttons, footer);
        content.setAlignment(Pos.TOP_LEFT);
        root.setCenter(content);

        return root;
    }

    private static Region spacer() {
        Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r;
    }

    public static void main(String[] args) { launch(args); }
}
