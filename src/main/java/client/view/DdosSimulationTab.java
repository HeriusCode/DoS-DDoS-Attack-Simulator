package client.view;

import client.model.SimulationState;
import client.model.TrafficStatistics;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Supplier;

/** DDoS simulation tab layout. */
final class DdosSimulationTab {
    interface Actions { void testConnection(); void exportLog(); void start(); void stop(); void reset(); }

    private final TargetPanel target;
    private final DDoSSimulationPanel controls;
    private final DdosStatisticsPanel statistics;
    private final LineChart<Number, Number> chart;
    private final TextArea log;
    private final TableView<TrafficStatistics.WorkerSnapshot> nodeTable;
    private final Label connection;
    private final Supplier<TrafficStatistics.Snapshot> snapshot;
    private final Supplier<String> simulationType;
    private final Supplier<SimulationState> simulationState;
    private final Supplier<Boolean> verified;
    private final Actions actions;

    DdosSimulationTab(TargetPanel target, DDoSSimulationPanel controls, DdosStatisticsPanel statistics,
                      LineChart<Number, Number> chart, TextArea log, TableView<TrafficStatistics.WorkerSnapshot> nodeTable,
                      Label connection, Supplier<TrafficStatistics.Snapshot> snapshot, Supplier<String> simulationType,
                      Supplier<SimulationState> simulationState, Supplier<Boolean> verified, Actions actions) {
        this.target = target; this.controls = controls; this.statistics = statistics; this.chart = chart; this.log = log;
        this.nodeTable = nodeTable; this.connection = connection; this.snapshot = snapshot; this.simulationType = simulationType;
        this.simulationState = simulationState; this.verified = verified; this.actions = actions;
    }

    Node build() {
        VBox root = new VBox(12); root.getStyleClass().add("ddos-view");
        HBox title = new HBox(14, CyberIcon.of(CyberIcon.Type.NETWORK, 42, "ddos-title-icon"),
                ViewSupport.pageHeader("DDoS SIMULATION", "Simulate multiple logical traffic workers against the verified lab server"));
        title.setAlignment(Pos.CENTER_LEFT); title.getStyleClass().add("ddos-page-header");
        VBox targetBox = panel("TARGET CONFIGURATION", targetForm());
        VBox controlsBox = panel("SIMULATION CONTROL", controls);
        HBox top = new HBox(12, targetBox, controlsBox);
        HBox.setHgrow(targetBox, Priority.ALWAYS); HBox.setHgrow(controlsBox, Priority.ALWAYS);
        targetBox.setPrefWidth(620); controlsBox.setPrefWidth(520);

        VBox realtime = panel("REAL-TIME STATISTICS", statistics, chart);
        VBox logPanel = logPanel();
        HBox middle = new HBox(12, realtime, logPanel);
        HBox.setHgrow(realtime, Priority.ALWAYS); HBox.setHgrow(logPanel, Priority.ALWAYS);
        realtime.setPrefWidth(680); logPanel.setPrefWidth(470);

        VBox nodes = panel("NODE STATUS", nodeTable);
        VBox result = resultPanel();
        VBox quickActions = quickActions();
        HBox bottom = new HBox(12, nodes, result, quickActions);
        HBox.setHgrow(nodes, Priority.ALWAYS); HBox.setHgrow(result, Priority.ALWAYS); HBox.setHgrow(quickActions, Priority.ALWAYS);
        nodes.setPrefWidth(480); result.setPrefWidth(350); quickActions.setPrefWidth(300);
        root.getChildren().addAll(title, top, middle, bottom);
        return ViewSupport.page(root);
    }

    private Node targetForm() {
        TextField host = new TextField(), port = new TextField();
        host.textProperty().bindBidirectional(target.host.textProperty()); port.textProperty().bindBidirectional(target.port.textProperty());
        ComboBox<String> protocol = new ComboBox<>(); protocol.getItems().add("HTTP"); protocol.setValue("HTTP");
        host.setMaxWidth(Double.MAX_VALUE); port.setMaxWidth(Double.MAX_VALUE); protocol.setMaxWidth(Double.MAX_VALUE);
        Button test = ViewSupport.successButton("TEST CONNECTION"); test.setOnAction(event -> actions.testConnection());
        Label connected = new Label(); connected.getStyleClass().add("ddos-connection");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> connected.setText(connection.getText())));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(7);
        for (int index = 0; index < 4; index++) { ColumnConstraints column = new ColumnConstraints(); column.setPercentWidth(25); column.setHgrow(Priority.ALWAYS); grid.getColumnConstraints().add(column); }
        grid.add(new Label("Target IP"), 0, 0); grid.add(new Label("Port"), 1, 0); grid.add(new Label("Protocol"), 2, 0);
        grid.add(host, 0, 1); grid.add(port, 1, 1); grid.add(protocol, 2, 1); grid.add(test, 3, 1); grid.add(connected, 3, 2);
        return grid;
    }

    private VBox logPanel() {
        Button clear = ViewSupport.smallButton("CLEAR"), export = ViewSupport.smallButton("EXPORT");
        clear.setOnAction(event -> log.clear()); export.setOnAction(event -> actions.exportLog());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(8, sectionTitle("SIMULATION LOG"), spacer, clear, export); tools.setAlignment(Pos.CENTER_LEFT);
        log.setPrefHeight(340);
        VBox panel = new VBox(9, tools, log); panel.getStyleClass().addAll("cyber-panel", "ddos-panel"); panel.setPadding(new Insets(10));
        VBox.setVgrow(log, Priority.ALWAYS); return panel;
    }

    private VBox resultPanel() {
        Label attack = ViewSupport.value("DDoS"), resultTarget = ViewSupport.value("-"), nodes = ViewSupport.value("-");
        Label rate = ViewSupport.value("-"), duration = ViewSupport.value("-");
        Label message = new Label("No DDoS simulation data yet"); message.getStyleClass().add("ddos-result-message");
        GridPane grid = new GridPane(); grid.setHgap(14); grid.setVgap(7);
        ViewSupport.addInfoRow(grid, 0, "Attack Type", attack); ViewSupport.addInfoRow(grid, 1, "Target", resultTarget);
        ViewSupport.addInfoRow(grid, 2, "Simulated Nodes", nodes); ViewSupport.addInfoRow(grid, 3, "Requests/sec/node", rate); ViewSupport.addInfoRow(grid, 4, "Duration", duration);
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            TrafficStatistics.Snapshot data = snapshot.get();
            if (data == null || !"DDoS".equals(simulationType.get())) return;
            resultTarget.setText(target.host.getText() + ":" + target.port.getText()); nodes.setText(controls.nodes.getText()); rate.setText(controls.rate.getText());
            duration.setText((data.elapsedSeconds() + data.remainingSeconds()) + " seconds");
            message.setText(simulationState.get() == SimulationState.RUNNING ? "DDoS simulation is running" : "Simulation completed");
        }));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        return panel("SIMULATION RESULT", grid, message);
    }

    private VBox quickActions() {
        Button start = ViewSupport.successButton("START"), stop = ViewSupport.dangerButton("STOP"), reset = ViewSupport.secondaryButton("RESET");
        start.setMaxWidth(Double.MAX_VALUE); stop.setMaxWidth(Double.MAX_VALUE); reset.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(event -> actions.start()); stop.setOnAction(event -> actions.stop()); reset.setOnAction(event -> actions.reset());
        Timeline enabled = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            boolean running = simulationState.get() == SimulationState.RUNNING;
            start.setDisable(running || !verified.get()); stop.setDisable(!running);
        }));
        enabled.setCycleCount(Animation.INDEFINITE); enabled.play();
        return panel("QUICK ACTIONS", start, stop, reset);
    }

    private VBox panel(String title, Node... children) { VBox panel = new VBox(10, sectionTitle(title)); panel.getChildren().addAll(children); panel.getStyleClass().addAll("cyber-panel", "ddos-panel"); panel.setPadding(new Insets(10)); return panel; }
    private Label sectionTitle(String text) { Label label = new Label(text); label.getStyleClass().add("ddos-section-title"); return label; }
}
