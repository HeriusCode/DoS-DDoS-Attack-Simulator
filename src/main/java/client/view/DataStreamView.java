package client.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.Random;

/** Decorative, locally rendered binary-rain animation for the dashboard. */
public final class DataStreamView extends StackPane {
    private static final double COLUMN_WIDTH = 16;
    private static final double ROW_HEIGHT = 15;
    private static final int TRAIL_LENGTH = 16;

    private final Canvas canvas = new Canvas();
    private final Random random = new Random(0xC0DEC0DEL);
    private final Timeline animation;
    private double[] drops = new double[0];

    public DataStreamView() {
        getStyleClass().add("data-stream-view");
        canvas.setManaged(false);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        widthProperty().addListener((observable, oldValue, newValue) -> ensureColumns());
        heightProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue.doubleValue() <= 1) initializeDrops();
            draw();
        });

        animation = new Timeline(new KeyFrame(Duration.millis(70), event -> advance()));
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
        initializeDrops();
        draw();
    }

    private void initializeDrops() {
        double height = Math.max(218, getHeight());
        for (int index = 0; index < drops.length; index++) {
            drops[index] = random.nextDouble() * (height + TRAIL_LENGTH * ROW_HEIGHT)
                    - TRAIL_LENGTH * ROW_HEIGHT * .45;
        }
    }

    private void advance() {
        if (drops.length == 0) ensureColumns();
        double limit = getHeight() + TRAIL_LENGTH * ROW_HEIGHT;
        for (int index = 0; index < drops.length; index++) {
            drops[index] += 2.4 + (index % 4) * .35;
            if (drops[index] > limit) {
                drops[index] = -random.nextInt((int) (getHeight() * .55 + 1));
            }
        }
        draw();
    }

    private void draw() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        graphics.setFill(Color.rgb(0, 8, 3));
        graphics.fillRect(0, 0, width, height);
        graphics.setFont(Font.font("Consolas", 13));

        for (int column = 0; column < drops.length; column++) {
            double x = column * COLUMN_WIDTH + 4;
            for (int trail = 0; trail < TRAIL_LENGTH; trail++) {
                double y = drops[column] - trail * ROW_HEIGHT;
                if (y < 0 || y > height) continue;

                double brightness = 1.0 - trail / (double) TRAIL_LENGTH;
                Color color = trail == 0
                        ? Color.rgb(190, 255, 215)
                        : Color.color(0.0, .24 + brightness * .68, .08, .10 + brightness * .80);
                graphics.setFill(color);
                graphics.fillText(random.nextBoolean() ? "1" : "0", x, y);
            }
        }

        graphics.setFill(Color.color(0, 1, .22, .055));
        graphics.fillRect(0,
                Math.floorMod((int) (System.nanoTime() / 8_000_000L), Math.max(1, (int) height)),
                width, 2);
    }
}
