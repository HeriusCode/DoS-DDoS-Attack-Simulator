package client.view;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/** Lightweight vector icons so the UI does not depend on an external icon font. */
public final class CyberIcon {
    public enum Type { SHIELD, DASHBOARD, SERVER, LIGHTNING, NETWORK, CHART, LOG, SETTINGS, TARGET, PLAY, STOP, RESET, EXPORT, TRASH }

    private CyberIcon() { }

    public static StackPane of(Type type, double size, String styleClass) {
        SVGPath svg = new SVGPath();
        svg.setContent(path(type));
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        svg.getStyleClass().add("icon-shape");
        StackPane box = new StackPane(svg);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.getStyleClass().addAll("cyber-icon", styleClass);
        return box;
    }

    private static String path(Type type) {
        return switch (type) {
            case SHIELD -> "M12 1 21 5v6c0 5.5-3.8 10.3-9 12-5.2-1.7-9-6.5-9-12V5l9-4zm0 3.1L6 6.7V11c0 3.9 2.4 7.4 6 8.8 3.6-1.4 6-4.9 6-8.8V6.7l-6-2.6z";
            case DASHBOARD -> "M3 3h8v8H3V3zm10 0h8v5h-8V3zM3 13h8v8H3v-8zm10-3h8v11h-8V10z";
            case SERVER -> "M3 3h18v5H3V3zm2 2v1h2V5H5zm-2 5h18v5H3v-5zm2 2v1h2v-1H5zm-2 5h18v4H3v-4zm2 1v1h2v-1H5z";
            case LIGHTNING -> "M13.5 1 4 13h6l-1 10 10-14h-6l.5-8z";
            case NETWORK -> "M12 2a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM4 16a3 3 0 1 1 0 6 3 3 0 0 1 0-6zm16 0a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM11 9h2v4.2l5.1 2.6-1 1.8-5.1-2.7-5.1 2.7-1-1.8 5.1-2.6V9z";
            case CHART -> "M3 20h18v2H1V3h2v17zm3-2V9h3v9H6zm5 0V4h3v14h-3zm5 0v-6h3v6h-3z";
            case LOG -> "M5 2h10l4 4v16H5V2zm9 2v4h4M8 11h8v2H8v-2zm0 4h8v2H8v-2zm0 4h6v2H8v-2z";
            case SETTINGS -> "M10.7 2h2.6l.5 2.2c.6.2 1.1.4 1.6.7l2-1 1.8 1.8-1 2c.3.5.5 1 .7 1.6l2.1.5v2.6l-2.1.5c-.2.6-.4 1.1-.7 1.6l1 2-1.8 1.8-2-1c-.5.3-1 .5-1.6.7l-.5 2.1h-2.6l-.5-2.1c-.6-.2-1.1-.4-1.6-.7l-2 1-1.8-1.8 1-2c-.3-.5-.5-1-.7-1.6L3 13.3v-2.6l2.1-.5c.2-.6.4-1.1.7-1.6l-1-2 1.8-1.8 2 1c.5-.3 1-.5 1.6-.7L10.7 2zM12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
            case TARGET -> "M11 2h2v3.1A7 7 0 0 1 18.9 11H22v2h-3.1a7 7 0 0 1-5.9 5.9V22h-2v-3.1A7 7 0 0 1 5.1 13H2v-2h3.1A7 7 0 0 1 11 5.1V2zm1 5a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 3a2 2 0 1 1 0 4 2 2 0 0 1 0-4z";
            case PLAY -> "M6 3v18l15-9L6 3z";
            case STOP -> "M5 5h14v14H5z";
            case RESET -> "M12 4a8 8 0 1 1-7.4 5H2l3.5-4L9 9H6.7A6 6 0 1 0 12 6V4z";
            case EXPORT -> "M11 3h2v9l3-3 1.4 1.4L12 16l-5.4-5.6L8 9l3 3V3zM4 18h16v3H4v-3z";
            case TRASH -> "M7 7h2v12H7V7zm4 0h2v12h-2V7zm4 0h2v12h-2V7zM5 5h14v2H5V5zm3-3h8l1 2H7l1-2z";
        };
    }
}
