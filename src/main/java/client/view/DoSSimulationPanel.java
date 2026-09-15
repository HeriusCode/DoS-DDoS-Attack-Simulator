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

/** Input-only DoS controls. Execution remains in AttackController. */
public final class DoSSimulationPanel extends VBox {
    public final TextField rate = new TextField("5");
    public final TextField duration = new TextField("15");
    public final Button start = new Button("START");
    public final Button stop = new Button("STOP");
    public final Label state = new Label("STOPPED");

    public DoSSimulationPanel() {
        setSpacing(16);
        getStyleClass().add("dos-controls");

        GridPane fields = new GridPane();
        fields.setHgap(18);
        fields.setVgap(8);
        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        column.setPercentWidth(50);
        fields.getColumnConstraints().addAll(column, column);
        fields.add(new Label("Requests/sec"), 0, 0);
        fields.add(new Label("Duration (seconds)"), 1, 0);
        fields.add(rate, 0, 1);
        fields.add(duration, 1, 1);
        rate.setMaxWidth(Double.MAX_VALUE);
        duration.setMaxWidth(Double.MAX_VALUE);

        Label dot = new Label("●");
        dot.getStyleClass().add("dos-status-dot");
        state.getStyleClass().add("dos-state");
        HBox status = new HBox(9, new Label("Status"), dot, state);
        status.setAlignment(Pos.CENTER_LEFT);

        start.setMaxWidth(Double.MAX_VALUE);
        stop.setMaxWidth(Double.MAX_VALUE);
        HBox buttons = new HBox(13, start, stop);
        HBox.setHgrow(start, Priority.ALWAYS);
        HBox.setHgrow(stop, Priority.ALWAYS);
        getChildren().addAll(fields, status, buttons);
    }
}
