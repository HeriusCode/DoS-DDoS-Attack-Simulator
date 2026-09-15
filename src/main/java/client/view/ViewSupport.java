package client.view;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Shared, stateless widgets used by the independently managed tab views. */
final class ViewSupport {
    private ViewSupport() { }

    static VBox panel(String title, Node... content) {
        VBox panel = new VBox(10, sectionTitle(title));
        panel.getChildren().addAll(content);
        panel.getStyleClass().add("cyber-panel");
        panel.setPadding(new javafx.geometry.Insets(11));
        return panel;
    }

    static Label sectionTitle(String text) {
        Label label = new Label(text, CyberIcon.of(iconFor(text), 16, "section-icon"));
        label.getStyleClass().add("section-title");
        return label;
    }

    static Label value(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("value-text");
        return label;
    }

    static VBox pageHeader(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label detail = new Label(subtitle);
        detail.getStyleClass().add("page-subtitle");
        return new VBox(3, heading, detail);
    }

    static ScrollPane page(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dashboard-scroll");
        return scroll;
    }

    static Button successButton(String text) {
        Button button = new Button(text);
        button.setGraphic(CyberIcon.of(text.contains("TEST") ? CyberIcon.Type.TARGET : CyberIcon.Type.PLAY, 15, "button-icon"));
        button.getStyleClass().addAll("cyber-button", "cyber-button-success");
        return button;
    }

    static Button dangerButton(String text) {
        Button button = new Button(text);
        button.setGraphic(CyberIcon.of(CyberIcon.Type.STOP, 14, "button-icon"));
        button.getStyleClass().addAll("cyber-button", "cyber-button-danger");
        return button;
    }

    static Button secondaryButton(String text) {
        CyberIcon.Type icon = text.contains("RESET") ? CyberIcon.Type.RESET
                : text.contains("EXPORT") ? CyberIcon.Type.EXPORT
                : text.contains("CLEAR") ? CyberIcon.Type.TRASH : CyberIcon.Type.TARGET;
        Button button = new Button(text, CyberIcon.of(icon, 14, "button-icon"));
        button.getStyleClass().addAll("cyber-button", "cyber-button-secondary");
        return button;
    }

    static Button smallButton(String text) {
        Button button = secondaryButton(text);
        button.getStyleClass().add("small-button");
        return button;
    }

    static void addInfoRow(GridPane grid, int row, String name, Label value) {
        Label key = new Label(name);
        key.getStyleClass().add("info-key");
        grid.add(key, 0, row);
        grid.add(value, 1, row);
    }

    private static CyberIcon.Type iconFor(String title) {
        String value = title.toUpperCase();
        if (value.contains("LOG")) return CyberIcon.Type.LOG;
        if (value.contains("TARGET") || value.contains("SERVER")) return CyberIcon.Type.TARGET;
        if (value.contains("STAT") || value.contains("RATE")) return CyberIcon.Type.CHART;
        if (value.contains("QUICK")) return CyberIcon.Type.LIGHTNING;
        if (value.contains("SIMULATION")) return CyberIcon.Type.NETWORK;
        return CyberIcon.Type.DASHBOARD;
    }
}
