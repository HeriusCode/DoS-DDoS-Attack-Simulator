package client.view;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Scrollable log that retains a plain-text export while coloring each severity level. */
final class ColoredLogView extends ScrollPane {
    private final VBox entries = new VBox(3);
    private final StringBuilder plainText = new StringBuilder();

    ColoredLogView() {
        entries.getStyleClass().add("colored-log-entries");
        setContent(entries);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("colored-log-view");
    }

    void append(String time, String level, String message) {
        String normalized = level == null ? "INFO" : level.toUpperCase();
        plainText.append('[').append(time).append("] [").append(normalized).append("] ")
                .append(message).append(System.lineSeparator());

        Text timestamp = text("[" + time + "]  ", "colored-log-time");
        Text severity = text("[" + normalized + "]  ", "colored-log-level", "colored-log-" + normalized.toLowerCase());
        Text detail = text(message, "colored-log-message");
        TextFlow line = new TextFlow(timestamp, severity, detail);
        line.getStyleClass().add("colored-log-line");
        entries.getChildren().add(line);
        Platform.runLater(() -> setVvalue(1));
    }

    void clear() {
        entries.getChildren().clear();
        plainText.setLength(0);
    }

    String getText() {
        return plainText.toString();
    }

    private Text text(String value, String... classes) {
        Text text = new Text(value);
        text.getStyleClass().addAll(classes);
        return text;
    }
}
