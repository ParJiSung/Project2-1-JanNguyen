package UI;
import javafx.scene.control.Button;

public final class Buttons {
  private Buttons() {}

  public static Button primary(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("btn-primary");
    b.setMaxWidth(220);
    b.setPrefHeight(48);
    return b;
  }

  public static Button secondary(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("btn-secondary");
    b.setMaxWidth(220);
    b.setPrefHeight(44);
    return b;
  }

  public static Button ghost(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("btn-ghost");
    b.setMaxWidth(220);
    b.setPrefHeight(40);
    return b;
  }
}
