package client.view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;

/** Pulsing nodes, targeting reticles and moving packets layered over the DDoS world map. */
final class AnimatedThreatOverlay extends Pane {
    private static final double[][] NODES = {
            {.16, .43}, {.23, .37}, {.25, .52}, {.31, .72},
            {.47, .37}, {.51, .45}, {.50, .61}, {.58, .50},
            {.66, .54}, {.73, .59}, {.80, .70}, {.86, .43}, {.87, .77}
    };
    private static final int[][] ROUTES = {
            {0, 4}, {1, 5}, {2, 7}, {3, 6}, {4, 8}, {5, 9},
            {6, 10}, {7, 11}, {8, 12}, {9, 11}, {1, 9}, {3, 8}
    };
    private static final double[][] SCAN_TARGETS = {
            {.21, .40}, {.48, .38}, {.69, .53}, {.86, .42},
            {.78, .69}, {.51, .61}, {.31, .70}, {.25, .50}
    };
    private static final double SCAN_MOVE_SECONDS = 3.2;
    private static final double SCAN_HOLD_SECONDS = 2.0;
    private static final double SCAN_CYCLE_SECONDS = SCAN_MOVE_SECONDS + SCAN_HOLD_SECONDS;

    private final Canvas canvas = new Canvas();
    private long previousFrame;
    private double time;

    AnimatedThreatOverlay() {
        getStyleClass().add("ddos-threat-overlay");
        setMouseTransparent(true);
        canvas.setManaged(false);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        widthProperty().addListener((observable, oldValue, newValue) -> draw());
        heightProperty().addListener((observable, oldValue, newValue) -> draw());

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (getScene() == null || !isVisible() || getWidth() < 2 || getHeight() < 2) {
                    previousFrame = now;
                    return;
                }
                double elapsed = previousFrame == 0 ? 0
                        : Math.min((now - previousFrame) / 1_000_000_000.0, .05);
                previousFrame = now;
                time = (time + elapsed) % 120.0;
                draw();
            }
        }.start();
    }

    private void draw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width < 2 || height < 2) return;
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        graphics.setGlobalBlendMode(BlendMode.ADD);

        drawRoutes(graphics, width, height);
        drawPackets(graphics, width, height);
        drawNodes(graphics, width, height);
        graphics.setGlobalBlendMode(BlendMode.SRC_OVER);
        drawScanner(graphics, width, height);
        graphics.setGlobalBlendMode(BlendMode.ADD);
        drawHudLabels(graphics, width, height);

        graphics.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    private void drawRoutes(GraphicsContext graphics, double width, double height) {
        graphics.setLineWidth(.75);
        for (int index = 0; index < ROUTES.length; index++) {
            double[] start = point(ROUTES[index][0], width, height);
            double[] end = point(ROUTES[index][1], width, height);
            double controlX = (start[0] + end[0]) * .5;
            double controlY = Math.min(start[1], end[1]) - height * (.08 + (index % 3) * .018);
            double pulse = .5 + .5 * Math.sin(time * 1.7 + index * .8);
            graphics.setStroke(Color.web(index % 4 == 0 ? "#ff9a3d" : "#ff3157", .10 + pulse * .11));
            graphics.beginPath();
            graphics.moveTo(start[0], start[1]);
            graphics.quadraticCurveTo(controlX, controlY, end[0], end[1]);
            graphics.stroke();
        }
    }

    private void drawPackets(GraphicsContext graphics, double width, double height) {
        for (int index = 0; index < ROUTES.length; index++) {
            double[] start = point(ROUTES[index][0], width, height);
            double[] end = point(ROUTES[index][1], width, height);
            double controlX = (start[0] + end[0]) * .5;
            double controlY = Math.min(start[1], end[1]) - height * (.08 + (index % 3) * .018);
            double progress = (time * (.13 + (index % 4) * .012) + index * .19) % 1.0;
            double inverse = 1.0 - progress;
            double x = inverse * inverse * start[0] + 2 * inverse * progress * controlX + progress * progress * end[0];
            double y = inverse * inverse * start[1] + 2 * inverse * progress * controlY + progress * progress * end[1];
            double radius = index % 3 == 0 ? 2.4 : 1.6;
            graphics.setFill(Color.web(index % 3 == 0 ? "#ffd27a" : "#ff405f", .85));
            graphics.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            graphics.setFill(Color.web("#ff1744", .16));
            graphics.fillOval(x - radius * 3.2, y - radius * 3.2, radius * 6.4, radius * 6.4);
        }
    }

    private void drawNodes(GraphicsContext graphics, double width, double height) {
        for (int index = 0; index < NODES.length; index++) {
            double[] point = point(index, width, height);
            double pulse = .5 + .5 * Math.sin(time * (2.2 + index % 3 * .28) + index * .72);
            double radius = 2.0 + pulse * 2.2;
            graphics.setFill(Color.web(index % 5 == 0 ? "#ffd06a" : "#ff3157", .60 + pulse * .38));
            graphics.fillOval(point[0] - radius, point[1] - radius, radius * 2, radius * 2);
            graphics.setStroke(Color.web("#ff3157", .18 + pulse * .46));
            graphics.setLineWidth(.8);
            graphics.strokeOval(point[0] - radius * 2.3, point[1] - radius * 2.3,
                    radius * 4.6, radius * 4.6);
        }
    }

    private void drawScanner(GraphicsContext graphics, double width, double height) {
        int targetIndex = Math.floorMod((int) (time / SCAN_CYCLE_SECONDS), SCAN_TARGETS.length);
        int previousIndex = Math.floorMod(targetIndex - 1, SCAN_TARGETS.length);
        double phase = time % SCAN_CYCLE_SECONDS;
        boolean locked = phase >= SCAN_MOVE_SECONDS;
        double progress = Math.min(1.0, phase / SCAN_MOVE_SECONDS);
        double eased = progress * progress * (3.0 - 2.0 * progress);
        double xRatio = lerp(SCAN_TARGETS[previousIndex][0], SCAN_TARGETS[targetIndex][0], eased);
        double yRatio = lerp(SCAN_TARGETS[previousIndex][1], SCAN_TARGETS[targetIndex][1], eased);
        double x = xRatio * width;
        double y = yRatio * height;

        double linePulse = .5 + .5 * Math.sin(time * 4.2);
        graphics.setLineWidth(.7);
        graphics.setStroke(Color.web("#6ee8f1", .30 + linePulse * .18));
        graphics.strokeLine(x, 2, x, height - 2);
        graphics.strokeLine(2, y, width - 2, y);
        graphics.setLineWidth(3.0);
        graphics.setStroke(Color.web("#29b7ca", .06 + linePulse * .05));
        graphics.strokeLine(x, 2, x, height - 2);
        graphics.strokeLine(2, y, width - 2, y);

        graphics.setStroke(Color.web("#9cf7ff", locked ? .92 : .48));
        graphics.setLineWidth(.9);
        graphics.strokeLine(x - 13, y, x - 4, y);
        graphics.strokeLine(x + 4, y, x + 13, y);
        graphics.strokeLine(x, y - 13, x, y - 4);
        graphics.strokeLine(x, y + 4, x, y + 13);

        if (locked) drawLockedTarget(graphics, x, y, xRatio, yRatio, phase - SCAN_MOVE_SECONDS);
    }

    private void drawLockedTarget(GraphicsContext graphics, double x, double y,
                                  double xRatio, double yRatio, double holdTime) {
        double reveal = Math.min(1.0, holdTime / .20);
        double pulse = .5 + .5 * Math.sin(holdTime * 7.0);
        double inner = (6 + pulse * 1.8) * reveal;
        double outer = (12 + pulse * 3.0) * reveal;

        graphics.setFill(Color.web("#4eeeff", .08 + pulse * .08));
        graphics.fillOval(x - outer * 1.45, y - outer * 1.45, outer * 2.9, outer * 2.9);
        graphics.setStroke(Color.web("#93f8ff", .96));
        graphics.setLineWidth(1.25);
        graphics.strokeOval(x - inner, y - inner, inner * 2, inner * 2);
        graphics.setStroke(Color.web("#47d9e7", .72 + pulse * .25));
        graphics.strokeOval(x - outer, y - outer, outer * 2, outer * 2);

        double rotation = holdTime * 95.0;
        graphics.setStroke(Color.web("#b5fbff", .78));
        graphics.setLineWidth(1.4);
        graphics.strokeArc(x - outer - 3, y - outer - 3, (outer + 3) * 2, (outer + 3) * 2,
                rotation, 68, ArcType.OPEN);
        graphics.strokeArc(x - outer - 3, y - outer - 3, (outer + 3) * 2, (outer + 3) * 2,
                rotation + 180, 68, ArcType.OPEN);

        graphics.setFill(Color.web("#d7ffff", .94));
        graphics.fillOval(x - 1.8, y - 1.8, 3.6, 3.6);
        graphics.setFont(Font.font("Consolas", 6.8));
        graphics.setFill(Color.web("#7cecf4", .66));
        graphics.fillText("TARGET LOCK", x + outer + 5, y - 4);
        graphics.fillText(String.format("[%02d/%02d]", Math.round(xRatio * 99), Math.round(yRatio * 99)),
                x + outer + 5, y + 6);
    }

    private void drawHudLabels(GraphicsContext graphics, double width, double height) {
        graphics.setFont(Font.font("Consolas", 7.5));
        String[] labels = {"EXT[8080]", "NODE_ACTIVE", "SYN/ACK", "PKT:1024", "TRACE[07]"};
        for (int index = 0; index < labels.length; index++) {
            double x = 8 + Math.floorMod(index * 97, Math.max(20, (int) width - 78));
            double y = 14 + Math.floorMod(index * 41, Math.max(20, (int) height - 20));
            double blink = .5 + .5 * Math.sin(time * 2.4 + index * 1.3);
            graphics.setFill(Color.web(index % 2 == 0 ? "#62c7d7" : "#ff8a3d", .16 + blink * .22));
            graphics.fillText(labels[index], x, y);
        }
    }

    private double[] point(int index, double width, double height) {
        return new double[]{NODES[index][0] * width, NODES[index][1] * height};
    }

    private double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }
}
