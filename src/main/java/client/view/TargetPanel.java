package client.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class TargetPanel extends VBox {
    public final TextField host = new TextField("127.0.0.1");
    public final TextField port = new TextField("8080");
    public final Label status = new Label("NOT TESTED");
    public final Button test = new Button("TEST CONNECTION");
    public final ComboBox<String> protocol = new ComboBox<>();
    private final GridPane connectionMetrics = new GridPane();

    public TargetPanel() {
        setSpacing(18);
        setPadding(new Insets(12));
        getStyleClass().add("target-panel");
        setMaxHeight(Double.MAX_VALUE);

        protocol.getItems().add("HTTP");
        protocol.setValue("HTTP");
        protocol.setMaxWidth(Double.MAX_VALUE);
        host.setMaxWidth(Double.MAX_VALUE);
        port.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(18,
                fieldGroup(CyberIcon.Type.MONITOR, "Target IP Address", host,
                        "e.g. 192.168.1.20 or 127.0.0.1"),
                fieldGroup(CyberIcon.Type.PORT, "Port", port, "e.g. 8080, 80, 443"),
                fieldGroup(CyberIcon.Type.LINK, "Protocol", protocol, "Select protocol (HTTP)"));
        form.getStyleClass().add("target-form");

        Label connectionTitle = new Label("Connection Test");
        connectionTitle.getStyleClass().add("connection-title");
        Label connectionHelp = new Label("Check if the target server is reachable");
        connectionHelp.getStyleClass().add("form-hint");
        VBox connectionCopy = new VBox(3, connectionTitle, connectionHelp);
        HBox connectionHeader = new HBox(13, CyberIcon.of(CyberIcon.Type.WIFI, 30, "connection-icon"), connectionCopy);
        connectionHeader.setAlignment(Pos.CENTER_LEFT);

        test.setMaxWidth(240);
        test.setMinHeight(43);
        Label statusCaption = new Label("Connection Status");
        statusCaption.getStyleClass().add("connection-status-caption");
        status.getStyleClass().add("connection-status-value");
        VBox statusCopy = new VBox(3, statusCaption, status);
        Separator divider = new Separator(Orientation.VERTICAL);
        divider.getStyleClass().add("connection-status-divider");
        connectionMetrics.setHgap(18);
        connectionMetrics.setVgap(7);
        HBox.setHgrow(connectionMetrics, Priority.ALWAYS);
        HBox statusBox = new HBox(14, CyberIcon.of(CyberIcon.Type.CHECK_CIRCLE, 38, "status-shield"),
                statusCopy, divider, connectionMetrics);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.getStyleClass().add("connection-status-box");

        Region statusSpacer = new Region();
        VBox.setVgrow(statusSpacer, Priority.ALWAYS);
        getChildren().addAll(form, new Separator(), connectionHeader, test, statusSpacer, statusBox);
    }

    public void setConnectionMetrics(Label responseTime, Label httpStatus, Label lastChecked) {
        connectionMetrics.getChildren().clear();
        addConnectionMetric(0, "Response Time", responseTime);
        addConnectionMetric(1, "HTTP Status", httpStatus);
        addConnectionMetric(2, "Last Checked", lastChecked);
    }

    private void addConnectionMetric(int row, String name, Label value) {
        Label key = new Label(name);
        key.getStyleClass().add("connection-metric-key");
        value.getStyleClass().add("connection-metric-value");
        connectionMetrics.add(key, 0, row);
        connectionMetrics.add(value, 1, row);
    }

    private VBox fieldGroup(CyberIcon.Type icon, String label, Region control, String hint) {
        Label title = new Label(label);
        title.getStyleClass().add("form-label");
        HBox titleRow = new HBox(13, CyberIcon.of(icon, 24, "form-icon"), title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label help = new Label(hint);
        help.getStyleClass().add("form-hint");
        help.setMinWidth(205);
        help.setPrefWidth(225);
        Region indent = new Region();
        indent.setMinWidth(37);
        indent.setPrefWidth(37);
        control.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox inputRow = new HBox(14, indent, control, help);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        VBox group = new VBox(7, titleRow, inputRow);
        group.getStyleClass().add("target-field-group");
        return group;
    }
}
