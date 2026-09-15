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

/** DoS simulation tab layout and live result rendering. */
final class DosSimulationTab {
    interface Actions { void testConnection(); void exportLog(); }

    private final TargetPanel target;
    private final DoSSimulationPanel controls;
    private final DosStatisticsPanel statistics;
    private final LineChart<Number, Number> chart;
    private final TextArea log;
    private final Label connection;
    private final Supplier<TrafficStatistics.Snapshot> snapshot;
    private final Supplier<String> simulationType;
    private final Supplier<SimulationState> simulationState;
    private final Actions actions;

    DosSimulationTab(TargetPanel target, DoSSimulationPanel controls, DosStatisticsPanel statistics,
                     LineChart<Number, Number> chart, TextArea log, Label connection,
                     Supplier<TrafficStatistics.Snapshot> snapshot, Supplier<String> simulationType,
                     Supplier<SimulationState> simulationState, Actions actions) {
        this.target = target;
        this.controls = controls;
        this.statistics = statistics;
        this.chart = chart;
        this.log = log;
        this.connection = connection;
        this.snapshot = snapshot;
        this.simulationType = simulationType;
        this.simulationState = simulationState;
        this.actions = actions;
    }

    Node build() {
        VBox root = new VBox(14);
        root.getStyleClass().add("dos-view");
        HBox title = new HBox(14, CyberIcon.of(CyberIcon.Type.TARGET, 42, "dos-title-icon"),
                ViewSupport.pageHeader("DoS SIMULATION", "Simulate controlled single-source traffic against the verified lab server"));
        title.setAlignment(Pos.CENTER_LEFT);
        title.getStyleClass().add("dos-page-header");

        VBox targetBox = panel("TARGET SERVER", targetForm());
        VBox controlsBox = panel("SIMULATION CONTROLS", controls);
        HBox top = new HBox(14, targetBox, controlsBox);
        HBox.setHgrow(targetBox, Priority.ALWAYS);
        HBox.setHgrow(controlsBox, Priority.ALWAYS);
        targetBox.setPrefWidth(640);
        controlsBox.setPrefWidth(500);

        VBox realtime = panel("REAL-TIME STATISTICS", statistics, chart);
        VBox runtimeLog = logPanel();
        HBox middle = new HBox(14, realtime, runtimeLog);
        HBox.setHgrow(realtime, Priority.ALWAYS);
        HBox.setHgrow(runtimeLog, Priority.ALWAYS);
        realtime.setPrefWidth(680);
        runtimeLog.setPrefWidth(470);

        root.getChildren().addAll(title, top, middle, resultPanel());
        return ViewSupport.page(root);
    }

    private Node targetForm() {
        TextField host = new TextField();
        TextField port = new TextField();
        host.textProperty().bindBidirectional(target.host.textProperty());
        port.textProperty().bindBidirectional(target.port.textProperty());
        ComboBox<String> protocol = new ComboBox<>();
        protocol.getItems().add("HTTP"); protocol.setValue("HTTP"); protocol.setMaxWidth(Double.MAX_VALUE);
        host.setMaxWidth(Double.MAX_VALUE); port.setMaxWidth(Double.MAX_VALUE);
        Label state = new Label();
        state.getStyleClass().add("dos-connection");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> state.setText(connection.getText())));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        Button test = ViewSupport.secondaryButton("TEST CONNECTION");
        test.setOnAction(event -> actions.testConnection());

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        for (double width : new double[]{30, 20, 20, 30}) {
            ColumnConstraints column = new ColumnConstraints(); column.setPercentWidth(width); grid.getColumnConstraints().add(column);
        }
        grid.add(new Label("Target IP"), 0, 0); grid.add(new Label("Port"), 1, 0); grid.add(new Label("Protocol"), 2, 0);
        grid.add(host, 0, 1); grid.add(port, 1, 1); grid.add(protocol, 2, 1);
        VBox connectionBox = new VBox(3, state, new Label("Server reachability"));
        connectionBox.getStyleClass().add("dos-connection-box");
        grid.add(connectionBox, 3, 0, 1, 2); grid.add(test, 3, 2);
        return grid;
    }

    private VBox logPanel() {
        Button clear = ViewSupport.smallButton("CLEAR");
        Button export = ViewSupport.smallButton("EXPORT");
        clear.setOnAction(event -> log.clear()); export.setOnAction(event -> actions.exportLog());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, sectionTitle("LOG"), spacer, clear, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        log.setPrefHeight(340);
        VBox panel = new VBox(9, toolbar, log);
        panel.getStyleClass().addAll("cyber-panel", "dos-panel"); panel.setPadding(new Insets(10));
        VBox.setVgrow(log, Priority.ALWAYS);
        return panel;
    }

    private VBox resultPanel() {
        Label type = ViewSupport.value("DoS"), resultTarget = ViewSupport.value("-"), duration = ViewSupport.value("-");
        Label sent = ViewSupport.value("0"), success = ViewSupport.value("0"), failed = ViewSupport.value("0");
        Label limited = ViewSupport.value("0"), average = ViewSupport.value("0 ms"), rps = ViewSupport.value("0");
        Label notice = new Label("No DoS simulation data yet"); notice.getStyleClass().add("dos-result-notice");
        GridPane result = new GridPane(); result.setHgap(20); result.setVgap(7);
        addCell(result, 0, 0, "Attack Type", type); addCell(result, 0, 1, "Target", resultTarget); addCell(result, 0, 2, "Duration", duration);
        addCell(result, 1, 0, "Requests Sent", sent); addCell(result, 1, 1, "Successful", success); addCell(result, 1, 2, "Failed", failed);
        addCell(result, 2, 0, "Limited / Rejected", limited); addCell(result, 2, 1, "Average Response", average); addCell(result, 2, 2, "Average Requests/sec", rps);
        for (int index = 0; index < 3; index++) { ColumnConstraints c = new ColumnConstraints(); c.setPercentWidth(33.333); c.setHgrow(Priority.ALWAYS); result.getColumnConstraints().add(c); }
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            TrafficStatistics.Snapshot data = snapshot.get();
            if (data == null || !"DoS".equals(simulationType.get())) return;
            resultTarget.setText(target.host.getText() + ":" + target.port.getText());
            duration.setText((data.elapsedSeconds() + data.remainingSeconds()) + " seconds");
            sent.setText(Long.toString(data.sent())); success.setText(Long.toString(data.successful())); failed.setText(Long.toString(data.failed()));
            limited.setText(Long.toString(data.limited())); average.setText(String.format("%.1f ms", data.averageResponseMs())); rps.setText(String.format("%.1f", data.currentRate()));
            notice.setText(simulationState.get() == SimulationState.RUNNING ? "DoS simulation is running" : "DoS simulation result available");
        }));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        HBox body = new HBox(18, result, notice); HBox.setHgrow(result, Priority.ALWAYS);
        return panel("SIMULATION RESULT", body);
    }

    private VBox panel(String title, Node... children) {
        VBox panel = new VBox(11, sectionTitle(title)); panel.getChildren().addAll(children);
        panel.getStyleClass().addAll("cyber-panel", "dos-panel"); panel.setPadding(new Insets(11)); return panel;
    }

    private Label sectionTitle(String text) { Label label = new Label(text); label.getStyleClass().add("dos-section-title"); return label; }
    private void addCell(GridPane grid, int column, int row, String name, Label value) { grid.add(new VBox(2, new Label(name), value), column, row); }
}
