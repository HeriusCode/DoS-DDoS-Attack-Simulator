package client.view;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/** Lightweight vector icons so the UI does not depend on an external icon font. */
public final class CyberIcon {
    public enum Type {
        SHIELD, HOME, DASHBOARD, SERVER, MONITOR, PORT, LINK, WIFI, CHECK_CIRCLE,
        LOCK, CHIP, CHEVRON_RIGHT, LIGHTNING, NETWORK, CHART, LOG, SETTINGS,
        TARGET, SEND, WARNING, CLOCK, CLIPBOARD, PLAY, STOP, RESET, EXPORT, TRASH,
        LIST, INFO, TRAFFIC, ERROR, FILTER, SEARCH, CALENDAR, PULSE
    }

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
            case HOME -> "M12 2 2 10.2V22h7v-7h6v7h7V10.2L12 2zm0 3.1 7 5.7V19h-2v-6H7v6H5v-8.2l7-5.7z";
            case DASHBOARD -> "M3 3h8v8H3V3zm10 0h8v5h-8V3zM3 13h8v8H3v-8zm10-3h8v11h-8V10z";
            case SERVER -> "M3 3h18v5H3V3zm2 2v1h2V5H5zm-2 5h18v5H3v-5zm2 2v1h2v-1H5zm-2 5h18v4H3v-4zm2 1v1h2v-1H5z";
            case MONITOR -> "M3 4h18v13H3V4zm2 2v9h14V6H5zm6 11h2v2h4v2H7v-2h4v-2z";
            case PORT -> "M8 2h2v4h4V2h2v4h2a4 4 0 0 1 4 4v10H2V10a4 4 0 0 1 4-4h2V2zM4 10v8h16v-8H4zm3 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm5 1h6v2h-6v-2z";
            case LINK -> "M9.6 14.4 8.2 13l5.5-5.5a4 4 0 0 1 5.7 5.7l-3.1 3.1-1.4-1.4 3.1-3.1a2 2 0 1 0-2.9-2.9l-5.5 5.5zM14.4 9.6l1.4 1.4-5.5 5.5a4 4 0 0 1-5.7-5.7l3.1-3.1 1.4 1.4L6 12.2a2 2 0 1 0 2.9 2.9l5.5-5.5z";
            case WIFI -> "M1 9l2 2a13 13 0 0 1 18 0l2-2A16 16 0 0 0 1 9zm4 4 2 2a7 7 0 0 1 10 0l2-2a10 10 0 0 0-14 0zm4 4 3 3 3-3a4.2 4.2 0 0 0-6 0z";
            case CHECK_CIRCLE -> "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 2a8 8 0 1 1 0 16 8 8 0 0 1 0-16zm-1.8 12.6-4-4 1.4-1.4 2.6 2.6 6.2-6.2 1.4 1.4-7.6 7.6z";
            case LOCK -> "M6 10V7a6 6 0 0 1 12 0v3h2v12H4V10h2zm2 0h8V7a4 4 0 0 0-8 0v3zm4 3a2 2 0 0 0-1 3.7V19h2v-2.3A2 2 0 0 0 12 13z";
            case CHIP -> "M8 2h2v3h4V2h2v3h3v3h3v2h-3v4h3v2h-3v3h-3v3h-2v-3h-4v3H8v-3H5v-3H2v-2h3v-4H2V8h3V5h3V2zm-1 5v10h10V7H7zm3 3h4v4h-4v-4z";
            case CHEVRON_RIGHT -> "M8.6 4.6 16 12l-7.4 7.4L10 20.8l8.8-8.8L10 3.2 8.6 4.6z";
            case LIGHTNING -> "M13.5 1 4 13h6l-1 10 10-14h-6l.5-8z";
            case NETWORK -> "M12 2a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM4 16a3 3 0 1 1 0 6 3 3 0 0 1 0-6zm16 0a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM11 9h2v4.2l5.1 2.6-1 1.8-5.1-2.7-5.1 2.7-1-1.8 5.1-2.6V9z";
            case CHART -> "M3 20h18v2H1V3h2v17zm3-2V9h3v9H6zm5 0V4h3v14h-3zm5 0v-6h3v6h-3z";
            case LOG -> "M5 2h10l4 4v16H5V2zm9 2v4h4M8 11h8v2H8v-2zm0 4h8v2H8v-2zm0 4h6v2H8v-2z";
            case SETTINGS -> "M10.7 2h2.6l.5 2.2c.6.2 1.1.4 1.6.7l2-1 1.8 1.8-1 2c.3.5.5 1 .7 1.6l2.1.5v2.6l-2.1.5c-.2.6-.4 1.1-.7 1.6l1 2-1.8 1.8-2-1c-.5.3-1 .5-1.6.7l-.5 2.1h-2.6l-.5-2.1c-.6-.2-1.1-.4-1.6-.7l-2 1-1.8-1.8 1-2c-.3-.5-.5-1-.7-1.6L3 13.3v-2.6l2.1-.5c.2-.6.4-1.1.7-1.6l-1-2 1.8-1.8 2 1c.5-.3 1-.5 1.6-.7L10.7 2zM12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
            case TARGET -> "M11 2h2v3.1A7 7 0 0 1 18.9 11H22v2h-3.1a7 7 0 0 1-5.9 5.9V22h-2v-3.1A7 7 0 0 1 5.1 13H2v-2h3.1A7 7 0 0 1 11 5.1V2zm1 5a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 3a2 2 0 1 1 0 4 2 2 0 0 1 0-4z";
            case SEND -> "M2 3l20 9-20 9 4-8 9-1-9-1-4-8zm5.2 6.2L5.5 6.1 16.8 11 7.2 9.2zm0 5.6 9.6-1.8-11.3 4.9 1.7-3.1z";
            case WARNING -> "M12 2 1 21h22L12 2zm0 4 7.5 13h-15L12 6zm-1 4v5h2v-5h-2zm0 7v2h2v-2h-2z";
            case CLOCK -> "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 2a8 8 0 1 1 0 16 8 8 0 0 1 0-16zm-1 3v6l5 3 1-1.7-4-2.3V7h-2z";
            case CLIPBOARD -> "M9 2h6l1 2h3v18H5V4h3l1-2zm1.2 2-.5 1h4.6l-.5-1h-3.6zM8 9h8v2H8V9zm0 4h8v2H8v-2zm0 4h6v2H8v-2z";
            case PLAY -> "M6 3v18l15-9L6 3z";
            case STOP -> "M5 5h14v14H5z";
            case RESET -> "M12 4a8 8 0 1 1-7.4 5H2l3.5-4L9 9H6.7A6 6 0 1 0 12 6V4z";
            case EXPORT -> "M11 3h2v9l3-3 1.4 1.4L12 16l-5.4-5.6L8 9l3 3V3zM4 18h16v3H4v-3z";
            case TRASH -> "M7 7h2v12H7V7zm4 0h2v12h-2V7zm4 0h2v12h-2V7zM5 5h14v2H5V5zm3-3h8l1 2H7l1-2z";
            case LIST -> "M4 5a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm5 0h12v3H9V5zM4 11a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm5 0h12v3H9v-3zM4 17a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm5 0h12v3H9v-3z";
            case INFO -> "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 4a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3zm-2 5h3v7h2v2H9v-2h2v-5h-1v-2z";
            case TRAFFIC -> "M3 7h13l-3-3 1.4-1.4L20 8l-5.6 5.4L13 12l3-3H3V7zm18 10H8l3 3-1.4 1.4L4 16l5.6-5.4L11 12l-3 3h13v2z";
            case ERROR -> "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-4.2 5.8L12 10.6l4.2-4.2 1.4 1.4-4.2 4.2 4.2 4.2-1.4 1.4-4.2-4.2-4.2 4.2-1.4-1.4 4.2-4.2-4.2-4.2 1.4-1.4z";
            case FILTER -> "M2 4h20l-8 9v6l-4 2v-8L2 4zm4.5 2 5.5 6.2L17.5 6h-11z";
            case SEARCH -> "M10 3a7 7 0 1 0 4.9 12L21 21l-1.4 1.4-6.1-6.1A7 7 0 0 0 10 3zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10z";
            case CALENDAR -> "M6 2h2v3h8V2h2v3h3v17H3V5h3V2zM5 9v11h14V9H5zm2 2h3v3H7v-3zm5 0h3v3h-3v-3zm5 0h1v3h-1v-3zM7 16h3v2H7v-2zm5 0h3v2h-3v-2z";
            case PULSE -> "M1 13h5l2-6 4 12 3-9 2 3h6v2h-7l-1-1.5-3 9L8 12l-1 3H1v-2z";
        };
    }
}
