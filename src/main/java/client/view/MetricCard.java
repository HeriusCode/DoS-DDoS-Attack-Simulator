package client.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Reusable SOC-style metric tile with a compact glyph, title and large value. */
public final class MetricCard extends VBox {
    private final Label value = new Label("0");

    public MetricCard(CyberIcon.Type iconType, String title, String colorClass) {
        Node icon = CyberIcon.of(iconType, 27, colorClass);
        Label caption = new Label(title);
        caption.getStyleClass().add("metric-caption");
        caption.setWrapText(true);
        caption.setMinWidth(0);
        value.getStyleClass().addAll("metric-value", colorClass);

        VBox text = new VBox(3, caption, value);
        text.setMinWidth(0);
        HBox row = new HBox(12, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        getChildren().add(row);
        getStyleClass().add("metric-card");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public void setValue(String text) {
        value.setText(text);
    }
}
