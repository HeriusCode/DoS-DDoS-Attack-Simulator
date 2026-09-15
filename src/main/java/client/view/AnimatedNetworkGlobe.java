package client.view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

/** Animated, code-rendered earth and network activity visual. */
final class AnimatedNetworkGlobe extends Pane {
    private static final double AXIAL_TILT = Math.toRadians(-23.5);

    private record GeoPoint(double latitude, double longitude) { }
    private record Projected(double x, double y, double depth) { }

    private static final double[][] CONTINENTS = {
            {-168, 64, -150, 58, -135, 53, -125, 48, -118, 34, -103, 22, -82, 25,
                    -66, 45, -78, 55, -98, 70, -130, 72, -168, 64},
            {-81, 12, -72, 7, -64, -5, -58, -18, -61, -35, -70, -53, -76, -30,
                    -79, -8, -81, 12},
            {-10, 36, 2, 44, 18, 47, 31, 57, 58, 60, 88, 54, 112, 49, 137, 52,
                    154, 60, 171, 51, 150, 36, 122, 24, 105, 8, 80, 8, 66, 24,
                    44, 31, 33, 42, 16, 39, -10, 36},
            {-17, 35, 3, 37, 20, 32, 33, 17, 42, 2, 35, -18, 24, -34, 12, -35,
                    1, -21, -9, 4, -14, 19, -17, 35},
            {112, -11, 130, -12, 145, -22, 153, -35, 138, -43, 119, -34, 112, -11},
            {-52, 82, -22, 78, -19, 65, -44, 59, -61, 70, -52, 82}
    };

    private static final GeoPoint[] NODES = {
            new GeoPoint(21.0, 105.8), new GeoPoint(35.7, 139.7),
            new GeoPoint(1.3, 103.8), new GeoPoint(51.5, -0.1),
            new GeoPoint(40.7, -74.0), new GeoPoint(37.8, -122.4),
            new GeoPoint(-33.9, 151.2), new GeoPoint(25.2, 55.3),
            new GeoPoint(-23.6, -46.6), new GeoPoint(48.9, 2.3),
            new GeoPoint(52.5, 13.4), new GeoPoint(19.4, -99.1),
            new GeoPoint(31.2, 121.5), new GeoPoint(28.6, 77.2),
            new GeoPoint(-1.3, 36.8), new GeoPoint(55.8, 37.6),
            new GeoPoint(37.6, 127.0), new GeoPoint(13.8, 100.5),
            new GeoPoint(-6.2, 106.8), new GeoPoint(6.5, 3.4),
            new GeoPoint(30.0, 31.2), new GeoPoint(-26.2, 28.0),
            new GeoPoint(43.7, -79.4), new GeoPoint(49.3, -123.1),
            new GeoPoint(-34.6, -58.4), new GeoPoint(-33.4, -70.7),
            new GeoPoint(41.0, 29.0), new GeoPoint(40.4, -3.7),
            new GeoPoint(41.9, 12.5), new GeoPoint(59.3, 18.1),
            new GeoPoint(22.3, 114.2), new GeoPoint(50.5, 30.5)
    };

    private static final int[][] LINKS = {
            {0, 1}, {0, 2}, {0, 7}, {1, 5}, {2, 6},
            {3, 4}, {3, 7}, {3, 9}, {4, 5}, {4, 8},
            {0, 12}, {0, 13}, {1, 12}, {2, 13}, {3, 10},
            {4, 11}, {7, 14}, {9, 15}, {10, 15}, {12, 13},
            {13, 14}, {6, 8}, {1, 16}, {0, 17}, {2, 18},
            {14, 19}, {7, 20}, {14, 21}, {4, 22}, {5, 23},
            {8, 24}, {8, 25}, {3, 26}, {9, 27}, {10, 28},
            {12, 30}, {15, 31}, {16, 30}, {17, 18}, {19, 20},
            {20, 21}, {22, 23}, {24, 25}, {26, 27}, {27, 28},
            {28, 29}, {29, 31}, {30, 31}, {4, 6}, {5, 6},
            {7, 24}, {3, 25}, {0, 4}, {12, 23}, {1, 24},
            {13, 22}, {18, 26}, {20, 11}, {30, 5}
    };

    private final Canvas canvas = new Canvas();
    /** Start with Europe and Africa facing the viewer so the earth reads immediately. */
    private double rotation = Math.PI / 2.0;
    private double pulse;
    private long previousFrame;

    AnimatedNetworkGlobe() {
    getStyleClass().add("animated-network-globe");

    // Giới hạn chiều cao của khu vực địa cầu
    setMinSize(190, 100);
    setPrefSize(220, 100);
    setMaxSize(Double.MAX_VALUE, 100);

    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());

    getChildren().add(canvas);

    new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (getScene() == null || !isVisible()
                    || getWidth() < 2 || getHeight() < 2) {
                previousFrame = now;
                return;
            }

            double elapsed = previousFrame == 0
                    ? 0
                    : Math.min(
                            (now - previousFrame) / 1_000_000_000.0,
                            0.05
                    );

            previousFrame = now;

            rotation = (rotation + elapsed * 0.23) % (Math.PI * 2);
            pulse = (pulse + elapsed * 0.42) % 1.0;

            draw();
        }
    }.start();
}

    private void draw() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        // Deliberately place the center just outside the lower-right corner.
        // This creates the close-up quarter-earth composition used in the reference.
        double centerX = width * 1.015;
        double centerY = height * 1.15;
        double radius = 120;

        graphics.clearRect(0, 0, width, height);
        graphics.setFill(new RadialGradient(0, 0, centerX, centerY, radius * 1.35, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00ff84", 0.16)),
                new Stop(0.65, Color.web("#007a42", 0.06)),
                new Stop(1, Color.TRANSPARENT)));
        graphics.fillOval(centerX - radius * 1.35, centerY - radius * 1.35,
                radius * 2.7, radius * 2.7);

        graphics.setFill(new RadialGradient(-35, 0.28, centerX - radius * 0.18,
                centerY - radius * 0.20, radius * 1.12, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#087c4c", 0.72)),
                new Stop(0.58, Color.web("#03452e", 0.66)),
                new Stop(1, Color.web("#001a12", 0.94))));
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        drawGrid(graphics, centerX, centerY, radius, false);
        drawContinents(graphics, centerX, centerY, radius);
        drawGrid(graphics, centerX, centerY, radius, true);
        drawNetwork(graphics, centerX, centerY, radius);

        graphics.setLineWidth(1.9);
        graphics.setStroke(Color.web("#00ff8c", 0.92));
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        graphics.setLineWidth(5.5);
        graphics.setStroke(Color.web("#00ff78", 0.10));
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private void drawGrid(GraphicsContext graphics, double centerX, double centerY,
                          double radius, boolean front) {
        graphics.setLineWidth(front ? 0.75 : 0.5);
        graphics.setStroke(Color.web(front ? "#35ff9a" : "#087344", front ? 0.52 : 0.18));

        for (int latitude = -60; latitude <= 60; latitude += 30) {
            for (int step = 0; step < 96; step++) {
                double lonA = -Math.PI + step * Math.PI * 2 / 96.0;
                double lonB = -Math.PI + (step + 1) * Math.PI * 2 / 96.0;
                Projected a = project(Math.toRadians(latitude), lonA, centerX, centerY, radius);
                Projected b = project(Math.toRadians(latitude), lonB, centerX, centerY, radius);
                drawByDepth(graphics, a, b, front);
            }
        }

        for (int longitude = -150; longitude <= 180; longitude += 30) {
            for (int step = 0; step < 48; step++) {
                double latA = -Math.PI / 2 + step * Math.PI / 48.0;
                double latB = -Math.PI / 2 + (step + 1) * Math.PI / 48.0;
                Projected a = project(latA, Math.toRadians(longitude), centerX, centerY, radius);
                Projected b = project(latB, Math.toRadians(longitude), centerX, centerY, radius);
                drawByDepth(graphics, a, b, front);
            }
        }
    }

    private void drawByDepth(GraphicsContext graphics, Projected a, Projected b, boolean front) {
        boolean isFront = (a.depth + b.depth) / 2.0 >= 0;
        if (isFront == front) graphics.strokeLine(a.x, a.y, b.x, b.y);
    }

    private void drawContinents(GraphicsContext graphics, double centerX, double centerY, double radius) {
        graphics.save();
        graphics.beginPath();
        graphics.arc(centerX, centerY, radius, radius, 0, 360);
        graphics.clip();
        for (double[] outline : CONTINENTS) {
            Projected[] points = new Projected[outline.length / 2];
            double averageDepth = 0;
            for (int index = 0; index < outline.length; index += 2) {
                Projected point = project(Math.toRadians(outline[index + 1]), Math.toRadians(outline[index]),
                        centerX, centerY, radius);
                points[index / 2] = point;
                averageDepth += point.depth;
            }
            averageDepth /= points.length;
            if (averageDepth < -0.18) continue;

            graphics.beginPath();
            graphics.moveTo(points[0].x, points[0].y);
            for (int index = 1; index < points.length; index++) {
                graphics.lineTo(points[index].x, points[index].y);
            }
            graphics.closePath();
            graphics.setFill(Color.web("#00e878", 0.25 + Math.max(0, averageDepth) * 0.18));
            graphics.fill();
            graphics.setStroke(Color.web("#6affab", 0.74));
            graphics.setLineWidth(1.15);
            graphics.stroke();
        }
        graphics.restore();
    }

    private void drawNetwork(GraphicsContext graphics, double centerX, double centerY, double radius) {
        for (int index = 0; index < LINKS.length; index++) {
            Projected start = project(NODES[LINKS[index][0]], centerX, centerY, radius);
            Projected end = project(NODES[LINKS[index][1]], centerX, centerY, radius);
            if (start.depth < -0.08 || end.depth < -0.08) continue;

            double midX = (start.x + end.x) / 2.0;
            double midY = (start.y + end.y) / 2.0;
            double fromCenterX = midX - centerX;
            double fromCenterY = midY - centerY;
            double length = Math.max(1, Math.hypot(fromCenterX, fromCenterY));
            // Long-distance connections arc well above the surface. The bright
            // packets below travel these full arcs, not a short surface segment.
            double lift = 18 + Math.hypot(end.x - start.x, end.y - start.y) * 0.28;
            double controlX = midX + fromCenterX / length * lift;
            double controlY = midY + fromCenterY / length * lift;

            graphics.beginPath();
            graphics.moveTo(start.x, start.y);
            graphics.quadraticCurveTo(controlX, controlY, end.x, end.y);
            graphics.setLineWidth(1.0);
            graphics.setStroke(Color.web("#69ffad", 0.52));
            graphics.stroke();

            for (int packet = 0; packet < 2; packet++) {
                double position = (pulse + index * 0.137 + packet * 0.5) % 1.0;
                double inverse = 1 - position;
                double packetX = inverse * inverse * start.x + 2 * inverse * position * controlX
                        + position * position * end.x;
                double packetY = inverse * inverse * start.y + 2 * inverse * position * controlY
                        + position * position * end.y;
                graphics.setFill(Color.web("#d2ffe2", 0.97));
                graphics.fillOval(packetX - 2.2, packetY - 2.2, 4.4, 4.4);
            }
        }

        for (GeoPoint node : NODES) {
            drawNode(graphics, project(node, centerX, centerY, radius), node.longitude, 1.0);
        }

        // A denser latitude/longitude node field makes the cropped earth look like
        // a global network rather than a handful of isolated city markers.
        int sequence = 0;
        for (int latitude = -54; latitude <= 54; latitude += 18) {
            for (int longitude = -165; longitude < 180; longitude += 24) {
                if ((sequence++ & 1) == 0) {
                    drawNode(graphics, project(Math.toRadians(latitude), Math.toRadians(longitude),
                            centerX, centerY, radius), longitude + latitude * 0.5, 0.58);
                }
            }
        }
    }

    private void drawNode(GraphicsContext graphics, Projected projected, double phase, double scale) {
        if (projected.depth < 0) return;
        double glow = (3.7 + Math.sin((pulse * Math.PI * 2) + phase * 0.05) * 1.0) * scale;
        graphics.setFill(Color.web("#00ff78", 0.23 * scale));
        graphics.fillOval(projected.x - glow, projected.y - glow, glow * 2, glow * 2);
        double core = 3.1 * scale;
        graphics.setFill(Color.web("#baffd0", 0.96));
        graphics.fillOval(projected.x - core / 2, projected.y - core / 2, core, core);
    }

    private Projected project(GeoPoint point, double centerX, double centerY, double radius) {
        return project(Math.toRadians(point.latitude), Math.toRadians(point.longitude),
                centerX, centerY, radius);
    }

    private Projected project(double latitude, double longitude,
                              double centerX, double centerY, double radius) {
        double rotatedLongitude = longitude + rotation;
        double latitudeRadius = Math.cos(latitude);
        double x = latitudeRadius * Math.cos(rotatedLongitude);
        double depth = latitudeRadius * Math.sin(rotatedLongitude);
        double y = -Math.sin(latitude);

        // Rotate the globe around its screen-horizontal axis. The land, grid and
        // network therefore keep the same natural axial tilt while the earth spins.
        double tiltedY = y * Math.cos(AXIAL_TILT) - depth * Math.sin(AXIAL_TILT);
        double tiltedDepth = y * Math.sin(AXIAL_TILT) + depth * Math.cos(AXIAL_TILT);
        return new Projected(centerX + x * radius, centerY + tiltedY * radius, tiltedDepth);
    }
}
