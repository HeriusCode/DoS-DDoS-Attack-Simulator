package client.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Target Server tab and its related panels. */
final class TargetServerTab {
    interface Actions {
        void testConnection();
        void selectPreset(String host, String port, String address);
    }

    private final TargetPanel target;
    private final Label connection;
    private final TextArea connectionLog;
    private final Actions actions;

    TargetServerTab(TargetPanel target, Label connection, TextArea connectionLog, Actions actions) {
        this.target = target;
        this.connection = connection;
        this.connectionLog = connectionLog;
        this.actions = actions;
    }

    Node build() {
        VBox root = new VBox(14, ViewSupport.pageHeader(CyberIcon.Type.TARGET,
                "TARGET SERVER", "Configure the target server (IP, Port, Protocol) and test connection"));
        VBox configuration = ViewSupport.panel("SERVER CONFIGURATION", CyberIcon.Type.SETTINGS, target);
        VBox right = new VBox(12, serverInformation(), quickPresets(), connectionLogPanel());
        VBox.setVgrow(right.getChildren().get(2), Priority.ALWAYS);
        HBox body = new HBox(14, configuration, right);
        body.setFillHeight(true);
        HBox.setHgrow(configuration, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(target, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        configuration.setMaxHeight(Double.MAX_VALUE);
        right.setMaxHeight(Double.MAX_VALUE);
        configuration.setPrefWidth(620);
        right.setPrefWidth(520);
        root.getChildren().add(body);
        return ViewSupport.page(root);
    }

    private VBox serverInformation() {
        Label ip = ViewSupport.value("");
        Label port = ViewSupport.value("");
        Label status = ViewSupport.value("");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            ip.setText(target.host.getText());
            port.setText(target.port.getText());
            status.setText(connection.getText());
        }));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();

        javafx.scene.layout.GridPane details = new javafx.scene.layout.GridPane();
        details.setHgap(18); details.setVgap(9);
        ViewSupport.addInfoRow(details, 0, CyberIcon.Type.MONITOR, "Target IP", ip);
        ViewSupport.addInfoRow(details, 1, CyberIcon.Type.PORT, "Port", port);
        ViewSupport.addInfoRow(details, 2, CyberIcon.Type.LINK, "Protocol", ViewSupport.value("HTTP"));
        ViewSupport.addInfoRow(details, 3, CyberIcon.Type.CHECK_CIRCLE, "Status", status);

        StackPane visual = new StackPane(new AnimatedNetworkGlobe());
visual.getStyleClass().add("server-visual");

HBox body = new HBox(22, details, visual);
HBox.setHgrow(details, Priority.ALWAYS);

body.setAlignment(Pos.TOP_LEFT);
body.setFillHeight(false);

visual.setAlignment(Pos.TOP_CENTER);
visual.setMaxHeight(100);

VBox panel = ViewSupport.panel(
        "SERVER INFORMATION",
        CyberIcon.Type.CHIP,
        body
);

panel.setMinHeight(0);
panel.setPrefHeight(150);
panel.setMaxHeight(155);

return panel;
    }

    private VBox quickPresets() {
        Button local = presetButton(CyberIcon.Type.MONITOR, "LOCAL TEST", "127.0.0.1:8080 (HTTP)", "127.0.0.1", "8080");
        Button lab = presetButton(CyberIcon.Type.SERVER, "LAB SERVER", "192.168.1.20:8080 (HTTP)", "192.168.1.20", "8080");
        return ViewSupport.panel("QUICK PRESETS", local, lab);
    }

    private Button presetButton(CyberIcon.Type icon, String title, String address, String host, String port) {
        Label name = new Label(title);
        name.getStyleClass().add("preset-name");
        Label value = new Label(address);
        value.getStyleClass().add("preset-address");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, CyberIcon.of(icon, 22, "preset-icon"), name, spacer, value,
                CyberIcon.of(CyberIcon.Type.CHEVRON_RIGHT, 13, "preset-chevron"));
        row.setAlignment(Pos.CENTER_LEFT);
        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("preset-button");
        button.setOnAction(event -> actions.selectPreset(host, port, address));
        return button;
    }

    private VBox connectionLogPanel() {
        Button clear = ViewSupport.smallButton("CLEAR");
        clear.setOnAction(event -> connectionLog.clear());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, ViewSupport.sectionTitle("RECENT CONNECTION LOGS", CyberIcon.Type.LOG), spacer, clear);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        connectionLog.setPrefHeight(190);
        VBox box = new VBox(9, toolbar, connectionLog);
        box.getStyleClass().add("cyber-panel");
        box.setPadding(new javafx.geometry.Insets(10));
        VBox.setVgrow(connectionLog, Priority.ALWAYS);
        return box;
    }
}
