package client.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Detailed target configuration form. Network actions are wired by MainDashboard. */
public final class TargetPanel extends VBox {
    public final TextField host = new TextField("127.0.0.1");
    public final TextField port = new TextField("8080");
    public final Label status = new Label("NOT TESTED");
    public final Button test = new Button("TEST CONNECTION");
    public final ComboBox<String> protocol = new ComboBox<>();

    public TargetPanel() {
        setSpacing(17);
        setPadding(new Insets(8));
        getStyleClass().add("target-panel");

        protocol.getItems().add("HTTP");
        protocol.setValue("HTTP");
        protocol.setMaxWidth(Double.MAX_VALUE);
        host.setMaxWidth(Double.MAX_VALUE);
        port.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(15);
        ColumnConstraints iconColumn = new ColumnConstraints(32);
        ColumnConstraints labelColumn = new ColumnConstraints(125);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints hintColumn = new ColumnConstraints(190);
        form.getColumnConstraints().addAll(iconColumn, labelColumn, fieldColumn, hintColumn);

        addRow(form, 0, CyberIcon.Type.DASHBOARD, "Target IP Address", host, "e.g. 192.168.1.20 or 127.0.0.1");
        addRow(form, 1, CyberIcon.Type.SERVER, "Port", port, "e.g. 8080, 80, 443");
        addRow(form, 2, CyberIcon.Type.TARGET, "Protocol", protocol, "Select protocol (HTTP)");

        Label connectionTitle = new Label("Connection Test");
        connectionTitle.getStyleClass().add("connection-title");
        Label connectionHelp = new Label("Check if the target server is reachable");
        connectionHelp.getStyleClass().add("form-hint");
        VBox connectionCopy = new VBox(3, connectionTitle, connectionHelp);
        HBox connectionHeader = new HBox(13, CyberIcon.of(CyberIcon.Type.NETWORK, 30, "connection-icon"), connectionCopy);
        connectionHeader.setAlignment(Pos.CENTER_LEFT);

        test.setMaxWidth(240);
        HBox statusBox = new HBox(14, CyberIcon.of(CyberIcon.Type.SHIELD, 38, "status-shield"),
                new VBox(3, new Label("Connection Status"), status));
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.getStyleClass().add("connection-status-box");

        getChildren().addAll(form, new Separator(), connectionHeader, test, statusBox);
    }

    private void addRow(GridPane grid, int row, CyberIcon.Type icon, String label,
                        javafx.scene.Node control, String hint) {
        Label title = new Label(label);
        title.getStyleClass().add("form-label");
        Label help = new Label(hint);
        help.getStyleClass().add("form-hint");
        grid.add(CyberIcon.of(icon, 23, "form-icon"), 0, row);
        grid.add(title, 1, row);
        grid.add(control, 2, row);
        grid.add(help, 3, row);
    }
}
