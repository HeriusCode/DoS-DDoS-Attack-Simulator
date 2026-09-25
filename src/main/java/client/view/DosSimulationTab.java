package client.view;

import client.model.SimulationState;
import client.model.TrafficStatistics;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.geometry.Orientation;
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
    private static final PseudoClass CONNECTED = PseudoClass.getPseudoClass("connected");

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
        VBox leftColumn = new VBox(10, title, targetBox);
        leftColumn.getStyleClass().add("dos-top-left-column");
        title.setMaxWidth(Double.MAX_VALUE);
        targetBox.setMaxWidth(Double.MAX_VALUE);

        HBox top = new HBox(14, leftColumn, controlsBox);
        top.setFillHeight(true);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(controlsBox, Priority.ALWAYS);
        leftColumn.setPrefWidth(640);
        controlsBox.setPrefWidth(500);
        controlsBox.setMaxHeight(Double.MAX_VALUE);

        VBox realtime = panel("REAL-TIME STATISTICS", statistics, chartBlock());
        VBox runtimeLog = logPanel();
        HBox middle = new HBox(14, realtime, runtimeLog);
        HBox.setHgrow(realtime, Priority.ALWAYS);
        HBox.setHgrow(runtimeLog, Priority.ALWAYS);
        realtime.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        runtimeLog.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        middle.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(middle, Priority.ALWAYS);
        realtime.setPrefWidth(680);
        runtimeLog.setPrefWidth(470);

        root.getChildren().addAll(top, middle, resultPanel());
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
        Button test = new Button("TEST CONNECTION", CyberIcon.of(CyberIcon.Type.LINK, 15, "button-icon"));
        test.getStyleClass().addAll("cyber-button", "dos-test-connection");
        test.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        test.setOnAction(event -> actions.testConnection());
        Timeline update = new Timeline(new KeyFrame(Duration.millis(250), event -> {
            String current = connection.getText().toLowerCase();
            boolean connected = current.contains("connected") && !current.contains("failed");
            test.setText(connected ? "CONNECTED" : "TEST CONNECTION");
            test.setGraphic(CyberIcon.of(connected ? CyberIcon.Type.CHECK_CIRCLE : CyberIcon.Type.LINK,
                    15, "button-icon"));
            test.pseudoClassStateChanged(CONNECTED, connected);
        }));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        for (double width : new double[]{30, 20, 20, 30}) {
            ColumnConstraints column = new ColumnConstraints(); column.setPercentWidth(width); grid.getColumnConstraints().add(column);
        }
        grid.add(new Label("Target IP"), 0, 0); grid.add(new Label("Port"), 1, 0); grid.add(new Label("Protocol"), 2, 0);
        grid.add(host, 0, 1); grid.add(port, 1, 1); grid.add(protocol, 2, 1);
        grid.add(test, 3, 0, 1, 2);
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

    private VBox chartBlock() {
        chart.setLegendVisible(false);
        Label chartTitle = new Label("Request Rate");
        chartTitle.getStyleClass().add("dos-chart-title");
        Label unit = new Label("(requests/sec)");
        unit.getStyleClass().add("dos-chart-unit");
        HBox title = new HBox(9, chartTitle, unit);
        title.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox legend = new HBox(18,
                legendItem("Current RPS", "dos-legend-current"),
                legendItem("Target RPS", "dos-legend-target"));
        legend.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(10, title, spacer, legend);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox block = new VBox(6, header, chart);
        block.getStyleClass().add("dos-chart-block");
        block.setMaxHeight(Double.MAX_VALUE);
        chart.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return block;
    }

    private HBox legendItem(String text, String colorClass) {
        Region line = new Region();
        line.getStyleClass().addAll("dos-legend-line", colorClass);
        Label label = new Label(text);
        label.getStyleClass().add("dos-legend-label");
        HBox item = new HBox(7, line, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private VBox resultPanel() {
        Label type = ViewSupport.value("DoS"), resultTarget = ViewSupport.value("-"), duration = ViewSupport.value("-");
        Label sent = ViewSupport.value("0"), success = ViewSupport.value("0"), failed = ViewSupport.value("0");
        Label limited = ViewSupport.value("0"), average = ViewSupport.value("0 ms"), rps = ViewSupport.value("0");
        Label workers = ViewSupport.value("0");

        VBox targetGroup = resultGroup(
                resultRow("Attack Type", type), resultRow("Target", resultTarget), resultRow("Duration", duration));
        VBox requestGroup = resultGroup(
                resultRow("Requests Sent", sent), resultRow("Successful", success),
                resultRow("Failed", failed), resultRow("Limited / Rejected", limited));
        VBox performanceGroup = resultGroup(
                resultRow("Average Response Time", average), resultRow("Average Requests/sec", rps),
                resultRow("Total Workers", workers));

        Separator firstDivider = resultDivider();
        Separator secondDivider = resultDivider();
        HBox resultData = new HBox(16, targetGroup, firstDivider, requestGroup, secondDivider, performanceGroup);
        resultData.setAlignment(Pos.TOP_LEFT);
        resultData.getStyleClass().add("dos-result-data");
        HBox.setHgrow(targetGroup, Priority.ALWAYS);
        HBox.setHgrow(requestGroup, Priority.ALWAYS);
        HBox.setHgrow(performanceGroup, Priority.ALWAYS);

        Label noticeTitle = new Label("No DoS simulation data yet",
                CyberIcon.of(CyberIcon.Type.WARNING, 22, "dos-result-icon"));
        noticeTitle.getStyleClass().add("dos-result-notice-title");
        Label noticeDetail = new Label("Start the simulation to see results here.");
        noticeDetail.getStyleClass().add("dos-result-notice-detail");
        VBox noticeCard = new VBox(12, noticeTitle, noticeDetail);
        noticeCard.getStyleClass().add("dos-result-notice-card");

        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            TrafficStatistics.Snapshot data = snapshot.get();
            if (data == null || !"DoS".equals(simulationType.get())) return;
            resultTarget.setText(target.host.getText() + ":" + target.port.getText());
            duration.setText((data.elapsedSeconds() + data.remainingSeconds()) + " seconds");
            sent.setText(Long.toString(data.sent())); success.setText(Long.toString(data.successful())); failed.setText(Long.toString(data.failed()));
            limited.setText(Long.toString(data.limited())); average.setText(String.format("%.1f ms", data.averageResponseMs())); rps.setText(String.format("%.1f", data.currentRate()));
            workers.setText(Integer.toString(data.activeWorkers()));
            boolean running = simulationState.get() == SimulationState.RUNNING;
            noticeTitle.setText(running ? "DoS simulation is running" : "DoS simulation result available");
            noticeDetail.setText(running ? "Live metrics are being collected." : "The latest simulation metrics are shown on the left.");
        }));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        HBox body = new HBox(12, resultData, noticeCard);
        body.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(resultData, Priority.ALWAYS);
        return panel("SIMULATION RESULT", body);
    }

    private VBox resultGroup(Node... rows) {
        VBox group = new VBox(8, rows);
        group.getStyleClass().add("dos-result-group");
        group.setMaxWidth(Double.MAX_VALUE);
        return group;
    }

    private HBox resultRow(String name, Label value) {
        Label key = new Label(name);
        key.getStyleClass().add("dos-result-key");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, key, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Separator resultDivider() {
        Separator divider = new Separator(Orientation.VERTICAL);
        divider.getStyleClass().add("dos-result-divider");
        return divider;
    }

    private VBox panel(String title, Node... children) {
        VBox panel = new VBox(11, sectionTitle(title)); panel.getChildren().addAll(children);
        panel.getStyleClass().addAll("cyber-panel", "dos-panel"); panel.setPadding(new Insets(11)); return panel;
    }

    private Label sectionTitle(String text) {
        CyberIcon.Type icon = switch (text) {
            case "TARGET SERVER" -> CyberIcon.Type.SERVER;
            case "SIMULATION CONTROLS" -> CyberIcon.Type.PLAY;
            case "REAL-TIME STATISTICS" -> CyberIcon.Type.CHART;
            case "LOG" -> CyberIcon.Type.LOG;
            case "SIMULATION RESULT" -> CyberIcon.Type.CLIPBOARD;
            default -> CyberIcon.Type.TARGET;
        };
        Label label = new Label(text, CyberIcon.of(icon, 18, "dos-section-icon"));
        label.getStyleClass().add("dos-section-title");
        return label;
    }
}
