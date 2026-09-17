package client.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class DDoSSimulationPanel extends VBox {
    public final TextField nodes = new TextField("2");
    public final TextField rate = new TextField("3");
    public final TextField duration = new TextField("15");
    public final Button start = new Button("START");
    public final Button stop = new Button("STOP");
    public final Label state = new Label("STOPPED");

    public DDoSSimulationPanel() {
        setSpacing(14);
        getStyleClass().add("ddos-controls");
        GridPane fields = new GridPane();
        fields.setHgap(12); fields.setVgap(7);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.333); column.setHgrow(Priority.ALWAYS);
            fields.getColumnConstraints().add(column);
        }
        fields.add(new Label("Simulated Nodes"), 0, 0);
        fields.add(new Label("Requests/sec per node"), 1, 0);
        fields.add(new Label("Duration (seconds)"), 2, 0);
        fields.add(nodes, 0, 1); fields.add(rate, 1, 1); fields.add(duration, 2, 1);
        nodes.setMaxWidth(Double.MAX_VALUE); rate.setMaxWidth(Double.MAX_VALUE); duration.setMaxWidth(Double.MAX_VALUE);

        Label dot = new Label("●"); dot.getStyleClass().add("ddos-status-dot");
        state.getStyleClass().add("ddos-state");
        HBox status = new HBox(9, new Label("Status"), dot, state);
        status.setAlignment(Pos.CENTER_LEFT);
        start.setMaxWidth(Double.MAX_VALUE); stop.setMaxWidth(Double.MAX_VALUE);
        HBox actions = new HBox(12, start, stop);
        HBox.setHgrow(start, Priority.ALWAYS); HBox.setHgrow(stop, Priority.ALWAYS);
        getChildren().addAll(fields, status, actions);
    }
}
