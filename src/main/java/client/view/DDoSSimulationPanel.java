package client.view;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** Input-only DDoS controls. Execution remains in AttackController. */
public final class DDoSSimulationPanel extends VBox {
    public final TextField nodes = new TextField("10");
    public final TextField rate = new TextField("100");
    public final TextField duration = new TextField("30");
    public final Button start = new Button("START");
    public final Button stop = new Button("STOP");
    public final Label state = new Label("STOPPED");
    public final Label elapsed = new Label("00:00");

    public DDoSSimulationPanel() {
        setSpacing(0);
        getStyleClass().add("ddos-controls");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(7);
        for (int index = 0; index < 3; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.333);
            column.setHgrow(Priority.ALWAYS);
            fields.getColumnConstraints().add(column);
        }
        fields.add(new Label("Simulated Nodes"), 0, 0);
        fields.add(new Label("Requests/sec per node"), 1, 0);
        fields.add(new Label("Duration (seconds)"), 2, 0);
        fields.add(nodes, 0, 1);
        fields.add(rate, 1, 1);
        fields.add(duration, 2, 1);
        nodes.setMaxWidth(Double.MAX_VALUE);
        rate.setMaxWidth(Double.MAX_VALUE);
        duration.setMaxWidth(Double.MAX_VALUE);

        Label dot = new Label("\u25CF");
        dot.getStyleClass().add("ddos-status-dot");
        state.getStyleClass().add("ddos-state");
        Label statusCaption = new Label("Status");
        statusCaption.getStyleClass().add("ddos-status-caption");
        elapsed.getStyleClass().add("ddos-elapsed");
        HBox statusValue = new HBox(9, dot, state);
        statusValue.setAlignment(Pos.CENTER_LEFT);
        VBox statusText = new VBox(5, statusCaption, statusValue, elapsed);
        statusText.setAlignment(Pos.TOP_LEFT);

        ImageView hacker = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/client/assets/ddos-status-hacker.png"))));
        hacker.setFitWidth(72);
        hacker.setFitHeight(76);
        hacker.setPreserveRatio(true);
        hacker.setMouseTransparent(true);
        hacker.getStyleClass().add("ddos-control-hacker");

        StackPane statusCard = new StackPane(hacker, statusText);
        StackPane.setAlignment(hacker, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(statusText, Pos.TOP_LEFT);
        statusCard.getStyleClass().add("ddos-control-status-card");

        start.setGraphic(CyberIcon.of(CyberIcon.Type.PLAY, 14, "button-icon"));
        stop.setGraphic(CyberIcon.of(CyberIcon.Type.STOP, 14, "button-icon"));
        start.setMaxWidth(Double.MAX_VALUE);
        stop.setMaxWidth(Double.MAX_VALUE);
        HBox buttons = new HBox(12, start, stop);
        HBox.setHgrow(start, Priority.ALWAYS);
        HBox.setHgrow(stop, Priority.ALWAYS);
        VBox inputArea = new VBox(15, fields, buttons);
        inputArea.getStyleClass().add("ddos-control-inputs");
        HBox.setHgrow(inputArea, Priority.ALWAYS);

        Separator divider = new Separator(Orientation.VERTICAL);
        divider.getStyleClass().add("ddos-control-divider");
        HBox body = new HBox(14, inputArea, divider, statusCard);
        body.setAlignment(Pos.TOP_LEFT);
        getChildren().add(body);
    }
}
