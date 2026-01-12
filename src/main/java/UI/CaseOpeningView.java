package UI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.MotionBlur;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

class CaseOpeningView {
    // --- constants (copied from your CaseOpeningFX) ---
    private static final double TILE_W = 140;
    private static final double TILE_H = 160;
    private static final double TRACK_SPACING = 12;

    private static final double VIEWPORT_W = 800;
    private static final double VIEWPORT_H = 260;

    private static final int REPEAT_SETS = 18; // how many times to repeat the item list in the track

    private final Random rng = new Random();

    // UI nodes / state
    private HBox track;               // scrolling strip of tiles
    private StackPane viewport;       // clipped view window
    private Group overlay;            // particles / pointer / banners
    private Button openBtn;
    private List<Item> baseItems;     // original item list (one set)
    private List<Node> tileNodes;     // nodes in the track, parallel to repeated items
    private Timeline spinTl;          // main spin animation

    // Public factory for embedding into a Scene or container
    public Parent createContent() {
        baseItems = createDefaultItems();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        Label title = new Label("Case Opening");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#e6e6e6"));
        root.setTop(title);
        BorderPane.setMargin(title, new Insets(0, 0, 12, 4));

        // Build viewport and track
        viewport = new StackPane();
        viewport.setPrefSize(VIEWPORT_W, VIEWPORT_H);
        viewport.setMaxSize(VIEWPORT_W, VIEWPORT_H);
        viewport.setMinSize(VIEWPORT_W, VIEWPORT_H);
        Rectangle clip = new Rectangle(VIEWPORT_W, VIEWPORT_H);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        viewport.setClip(clip);

        // background for viewport
        Region bg = new Region();
        bg.setPrefSize(VIEWPORT_W, VIEWPORT_H);
        bg.setStyle("-fx-background-color: linear-gradient(to bottom, #1f2937, #111827);");
        bg.setOpacity(0.95);

        // track (scrolling strip)
        track = new HBox(TRACK_SPACING);
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPadding(new Insets(10));

        // build tiles
        tileNodes = new ArrayList<>();
        populateTrack(baseItems);
        enableWrap();

        // pointer at center
        Polygon pointer = new Polygon();
        double px = VIEWPORT_W / 2.0;
        pointer.getPoints().addAll(
                px - 12, 6.0,
                px + 12, 6.0,
                px, 28.0
        );
        pointer.setFill(Color.web("#f59e0b"));
        pointer.setStroke(Color.web("#111827"));
        pointer.setStrokeWidth(1.2);
        pointer.setEffect(new DropShadow(10, Color.color(0,0,0,0.6)));

        overlay = new Group(pointer);

        StackPane trackLayer = new StackPane(bg, track, overlay);
        viewport.getChildren().add(trackLayer);

        // bottom controls
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);
        openBtn = new Button("Open Case");
        openBtn.setDefaultButton(true);
        openBtn.setPrefWidth(160);
        openBtn.setPrefHeight(40);
        openBtn.setStyle("-fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-color: #10b981; -fx-text-fill: white;");
        openBtn.setOnAction(e -> startSpin());

        Button refillBtn = new Button("Shuffle Items");
        refillBtn.setOnAction(e -> {
            if (spinTl != null) spinTl.stop();
            track.setTranslateX(0);
            tileNodes.clear();
            baseItems = shuffledCopy(baseItems);
            track.getChildren().clear();
            populateTrack(baseItems);
        });

        controls.getChildren().addAll(openBtn, refillBtn);

        VBox centerBox = new VBox(12, viewport, controls);
        centerBox.setAlignment(Pos.CENTER_LEFT);
        root.setCenter(centerBox);

        // scene styling (applied to root)
        root.setBackground(new Background(new BackgroundFill(Color.web("#0b1020"), CornerRadii.EMPTY, Insets.EMPTY)));

        return root;
    }

    // --- Model --------------------------------------------------------------

    enum Rarity {
        COMMON("Common", Color.web("#9ca3af"), 60),
        UNCOMMON("Uncommon", Color.web("#34d399"), 25),
        RARE("Rare", Color.web("#60a5fa"), 10),
        EPIC("Epic", Color.web("#a78bfa"), 4),
        LEGENDARY("Legendary", Color.web("#fbbf24"), 1);

        final String display; final Color color; final int weight;
        Rarity(String display, Color color, int weight) { this.display = display; this.color = color; this.weight = weight; }
    }

    static class Item {
        final String name; final Rarity rarity;
        Item(String name, Rarity rarity) { this.name = name; this.rarity = rarity; }
    }

    private List<Item> createDefaultItems() {
        return List.of(
                new Item("Urban Camo", Rarity.COMMON),
                new Item("Desert Stripe", Rarity.COMMON),
                new Item("Jungle Mist", Rarity.UNCOMMON),
                new Item("Neon Fade", Rarity.RARE),
                new Item("Crimson Howl", Rarity.EPIC),
                new Item("Gold Dragon", Rarity.LEGENDARY),
                new Item("Cobalt Core", Rarity.UNCOMMON),
                new Item("Sunset Wave", Rarity.RARE),
                new Item("Night Ops", Rarity.COMMON),
                new Item("Iridescent", Rarity.EPIC),
                new Item("Aurora", Rarity.RARE),
                new Item("Royal Relic", Rarity.LEGENDARY)
        );
    }

    private List<Item> shuffledCopy(List<Item> items) {
        List<Item> copy = new ArrayList<>(items); Collections.shuffle(copy, rng); return copy; }

    // --- View helpers -------------------------------------------------------

    private void populateTrack(List<Item> items) {
        // Repeat the base set to create a long scrolling track
        for (int r = 0; r < REPEAT_SETS; r++) {
            for (Item it : items) {
                Node tile = buildTile(it);
                track.getChildren().add(tile);
                tileNodes.add(tile);
            }
        }
    }

    private Node buildTile(Item item) {
        StackPane tile = new StackPane();
        tile.setPrefSize(TILE_W, TILE_H);
        tile.setMinSize(TILE_W, TILE_H);
        tile.setMaxSize(TILE_W, TILE_H);

        // base card
        Rectangle card = new Rectangle(TILE_W, TILE_H);
        card.setArcWidth(18);
        card.setArcHeight(18);
        card.setFill(gradientFor(item.rarity));
        card.setStroke(Color.color(0,0,0,0.45));

        // rarity stripe at bottom
        Rectangle stripe = new Rectangle(TILE_W, 10);
        stripe.setFill(item.rarity.color);
        stripe.setTranslateY(TILE_H/2.0 - 5);

        Label name = new Label(item.name);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0.2, 0, 1);");

        Label rarity = new Label(item.rarity.display);
        rarity.setTextFill(item.rarity.color);
        rarity.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        rarity.setTranslateY(26);

        // fake weapon silhouette placeholder
        Rectangle icon = new Rectangle(72, 20, Color.color(1,1,1,0.22));
        icon.setArcWidth(8);
        icon.setArcHeight(8);
        icon.setTranslateY(-18);

        tile.getChildren().addAll(card, icon, name, rarity, stripe);
        tile.setEffect(new DropShadow(8, Color.color(0,0,0,0.55)));

        return tile;
    }

    private Paint gradientFor(Rarity r) {
        Color tint = r.color.deriveColor(0, 1, 1.0, 0.22);
        return new LinearGradient(0, 0, 0, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, tint.interpolate(Color.web("#1f2937"), 0.5)),
                new Stop(1, tint.interpolate(Color.web("#0b1324"), 0.85)));
    }

    // --- Spin logic ---------------------------------------------------------

    /**
     * Enable endless wrapping so tiles appear to repeat forever while scrolling.
     * We recycle nodes from the front to the back (and vice versa) as the track translates.
     */
    private void enableWrap() {
        final double step = TILE_W + TRACK_SPACING;
        final double leftPadding = 10; // matches HBox padding left

        track.translateXProperty().addListener((obs, oldX, newX) -> {
            double t = newX.doubleValue();

            // Move first tiles to the end when they have fully passed the left edge
            while (-t - leftPadding > step) {
                Node first = track.getChildren().remove(0);
                track.getChildren().add(first);
                t += step; // keep visual position stable
            }
            // Move last tiles to the front if we ever move to the right
            while (-t - leftPadding < 0) {
                int last = track.getChildren().size() - 1;
                Node lastNode = track.getChildren().remove(last);
                track.getChildren().add(0, lastNode);
                t -= step;
            }
            if (t != newX.doubleValue()) { track.setTranslateX(t); }
        });
    }

    private void startSpin() {
        if (spinTl != null && spinTl.getStatus() == Animation.Status.RUNNING) return;

        openBtn.setDisable(true);

        // pick a winning item using rarity weights
        Item win = weightedPick(baseItems);

        // compute how many tile steps to travel: several full cycles + offset to the winning item
        int itemsPerSet = baseItems.size();
        int occurrence = baseItems.indexOf(win); // 0..itemsPerSet-1
        int extraSpins = 8 + rng.nextInt(5);    // 8..12 full cycles for drama
        int steps = extraSpins * itemsPerSet + occurrence;

        double step = TILE_W + TRACK_SPACING;
        double targetTranslate = track.getTranslateX() - steps * step; // move left by N steps

        // visual effects during spin
        MotionBlur blur = new MotionBlur(0, 0);
        track.setEffect(blur);

        DoubleProperty blurAmt = new SimpleDoubleProperty(0);
        blur.radiusProperty().bind(blurAmt);

        // duration scales with distance
        double distance = Math.abs(targetTranslate - track.getTranslateX());
        double pxPerSecondFast = 1800; // initial speed
        double seconds = Math.max(3.5, distance / pxPerSecondFast);

        // Timeline with deceleration (ease-out)
        spinTl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(track.translateXProperty(), track.getTranslateX(), Interpolator.LINEAR),
                        new KeyValue(blurAmt, 28, Interpolator.LINEAR)
                ),
                new KeyFrame(Duration.seconds(seconds * 0.7),
                        new KeyValue(blurAmt, 16, Interpolator.EASE_OUT)
                ),
                new KeyFrame(Duration.seconds(seconds),
                        new KeyValue(track.translateXProperty(), targetTranslate, Interpolator.SPLINE(0.15, 0.85, 0.1, 1.0)),
                        new KeyValue(blurAmt, 0, Interpolator.EASE_OUT)
                )
        );

        spinTl.setOnFinished(e -> {
            track.setEffect(null);
            Node winningNode = nodeUnderPointer();
            celebrate(winningNode, win);
            openBtn.setDisable(false);
        });

        spinTl.playFromStart();
    }

    /** Index of the tile currently under the pointer (within track.getChildren()). */
    private int indexUnderPointer() {
        final double leftPadding = 10;
        final double step = TILE_W + TRACK_SPACING;
        double x = -track.getTranslateX() - leftPadding + VIEWPORT_W / 2.0;
        return (int) Math.round(x / step);
    }

    /** Node under the pointer, considering wrapping. */
    private Node nodeUnderPointer() {
        int idx = indexUnderPointer();
        int n = track.getChildren().size();
        idx = ((idx % n) + n) % n; // positive modulo
        return track.getChildren().get(idx);
    }

    private Item weightedPick(List<Item> items) {
        int total = items.stream().mapToInt(i -> i.rarity.weight).sum();
        int r = rng.nextInt(total);
        int acc = 0;
        for (Item it : items) { acc += it.rarity.weight; if (r < acc) return it; }
        return items.get(items.size() - 1);
    }

    // --- Celebration & particles -------------------------------------------

    private void celebrate(Node winningNode, Item win) {
        // highlight the winning tile
        Glow glow = new Glow(0.0);
        DropShadow shadow = new DropShadow(20, win.rarity.color);
        winningNode.setEffect(new javafx.scene.effect.Blend(
                javafx.scene.effect.BlendMode.SRC_OVER, glow, shadow));

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.levelProperty(), 0.0),
                        new KeyValue(winningNode.scaleXProperty(), 1.0),
                        new KeyValue(winningNode.scaleYProperty(), 1.0)
                ),
                new KeyFrame(Duration.seconds(0.18),
                        new KeyValue(glow.levelProperty(), 0.85),
                        new KeyValue(winningNode.scaleXProperty(), 1.06),
                        new KeyValue(winningNode.scaleYProperty(), 1.06)
                ),
                new KeyFrame(Duration.seconds(0.36),
                        new KeyValue(glow.levelProperty(), 0.0),
                        new KeyValue(winningNode.scaleXProperty(), 1.0),
                        new KeyValue(winningNode.scaleYProperty(), 1.0)
                )
        );
        pulse.setCycleCount(3);
        pulse.play();

        // confetti burst
        emitConfetti(win.rarity.color);

        // banner label
        Label banner = new Label("You got: " + win.name + " (" + win.rarity.display + ")");
        banner.setTextFill(Color.WHITE);
        banner.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        banner.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 10; -fx-padding: 6 12 6 12;");
        overlay.getChildren().add(banner);
        StackPane.setAlignment(banner, Pos.BOTTOM_CENTER);
        StackPane.setMargin(banner, new Insets(0,0,10,0));

        FadeTransition ft = new FadeTransition(Duration.seconds(3.2), banner);
        ft.setFromValue(0); ft.setToValue(1); ft.setAutoReverse(true); ft.setCycleCount(2);
        ft.setOnFinished(e -> overlay.getChildren().remove(banner));
        ft.play();
    }

    private void emitConfetti(Color accent) {
        int pieces = 120;
        double centerX = VIEWPORT_W / 2.0;
        double topY = 10;

        Group confetti = new Group();
        overlay.getChildren().add(confetti);

        for (int i = 0; i < pieces; i++) {
            double size = 6 + rng.nextDouble() * 8;
            Rectangle rect = new Rectangle(size, size / 3.0);
            Color c = accent.interpolate(Color.WHITE, rng.nextDouble() * 0.6);
            rect.setFill(c);
            rect.setRotate(rng.nextDouble() * 360);
            rect.setTranslateX(centerX + (rng.nextDouble() - 0.5) * 40);
            rect.setTranslateY(topY);

            // fall
            double dx = (rng.nextDouble() - 0.5) * 260; // drift
            double dy = VIEWPORT_H - 20 + rng.nextDouble() * 80;
            Duration d = Duration.seconds(1.6 + rng.nextDouble() * 1.4);

            TranslateTransition tt = new TranslateTransition(d, rect);
            tt.setByX(dx); tt.setByY(dy); tt.setInterpolator(Interpolator.EASE_IN);

            RotateTransition rt = new RotateTransition(d, rect);
            rt.setByAngle(540 + rng.nextDouble() * 540);

            FadeTransition ft = new FadeTransition(d, rect);
            ft.setFromValue(1); ft.setToValue(0);

            ParallelTransition pt = new ParallelTransition(rect, tt, rt, ft);
            pt.setDelay(Duration.seconds(rng.nextDouble()*0.4));
            pt.setOnFinished(e -> confetti.getChildren().remove(rect));
            pt.play();

            confetti.getChildren().add(rect);
        }

        // cleanup container after a few seconds
        PauseTransition cleanup = new PauseTransition(Duration.seconds(3.5));
        cleanup.setOnFinished(e -> overlay.getChildren().remove(confetti));
        cleanup.play();
    }
}
