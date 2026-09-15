package client.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.Random;

/** Dim, locally rendered code-stream animation used behind the DDoS map. */
final class AnimatedCodeBackdrop extends Pane {
    private static final double COLUMN_WIDTH = 54;
    private static final double ROW_HEIGHT = 17;
    private static final int TRAIL_LENGTH = 9;
    private static final String[] TOKENS = {
            "101101", "0x4F", "ACK", "NODE_07", "{01}", "8080", "SYN",
            "110010", "GET /", "[24/7]", "0xA9", "DROP", "0101", "TLS"
    };

    private final Canvas canvas = new Canvas();
    private final Random random = new Random(0xDD05C0DEL);
    private final Timeline animation;
    private double[] drops = new double[0];
    private double[] speeds = new double[0];
    private long frame;

    AnimatedCodeBackdrop() {
        getStyleClass().add("ddos-code-backdrop");
        canvas.setManaged(false);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        widthProperty().addListener((observable, oldValue, newValue) -> ensureColumns());
        heightProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue.doubleValue() <= 1) initializeStreams();
            draw();
        });

        animation = new Timeline(new KeyFrame(Duration.millis(85), event -> advance()));
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    private void ensureColumns() {
        int count = Math.max(1, (int) Math.ceil(getWidth() / COLUMN_WIDTH));
        if (count == drops.length) {
            draw();
            return;
        }
        drops = new double[count];
        speeds = new double[count];
        initializeStreams();
        draw();
    }

    private void initializeStreams() {
        double height = Math.max(190, getHeight());
        for (int index = 0; index < drops.length; index++) {
            drops[index] = random.nextDouble() * (height + TRAIL_LENGTH * ROW_HEIGHT);
            speeds[index] = 1.1 + random.nextDouble() * 1.7;
        }
    }

    private void advance() {
        if (getScene() == null || !isVisible()) return;
        if (drops.length == 0) ensureColumns();
        frame++;
        double limit = getHeight() + TRAIL_LENGTH * ROW_HEIGHT;
        for (int index = 0; index < drops.length; index++) {
            drops[index] += speeds[index];
            if (drops[index] > limit) {
                drops[index] = -random.nextDouble() * Math.max(40, getHeight() * .65);
                speeds[index] = 1.1 + random.nextDouble() * 1.7;
            }
        }
        draw();
    }

    private void draw() {
        double width = Math.max(1, canvas.getWidth());
        double height = Math.max(1, canvas.getHeight());
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);

        graphics.setStroke(Color.web("#ff3157", .055));
        graphics.setLineWidth(.55);
        for (double y = 10; y < height; y += 20) graphics.strokeLine(0, y, width, y);
        for (double x = 12; x < width; x += 44) graphics.strokeLine(x, 0, x, height);

        graphics.setFont(Font.font("Consolas", 9));
        for (int column = 0; column < drops.length; column++) {
            double x = column * COLUMN_WIDTH + 5;
            for (int trail = 0; trail < TRAIL_LENGTH; trail++) {
                double y = drops[column] - trail * ROW_HEIGHT;
                if (y < -ROW_HEIGHT || y > height + ROW_HEIGHT) continue;
                double fade = 1.0 - trail / (double) TRAIL_LENGTH;
                double opacity = trail == 0 ? .30 : .035 + fade * .13;
                graphics.setFill(column % 4 == 0
                        ? Color.web("#57c7d8", opacity * .65)
                        : Color.web("#ff3157", opacity));
                int tokenIndex = Math.floorMod(column * 5 + trail * 3 + (int) (frame / 18), TOKENS.length);
                graphics.fillText(TOKENS[tokenIndex], x, y);
            }
        }

        double scanY = Math.floorMod((int) (frame * 2), Math.max(1, (int) height));
        graphics.setFill(Color.web("#ff1744", .07));
        graphics.fillRect(0, scanY, width, 2);
    }
}
