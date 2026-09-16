package client.view;

import client.model.SimulationState;
import client.model.TrafficStatistics;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
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
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Supplier;
import java.util.Objects;

/** DDoS simulation tab layout. */
final class DdosSimulationTab {
    private static final PseudoClass CONNECTED = PseudoClass.getPseudoClass("connected");

    interface Actions { void testConnection(); void exportLog(); }

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
    private final Actions actions;

    DdosSimulationTab(TargetPanel target, DDoSSimulationPanel controls, DdosStatisticsPanel statistics,
                      LineChart<Number, Number> chart, TextArea log, TableView<TrafficStatistics.WorkerSnapshot> nodeTable,
                      Label connection, Supplier<TrafficStatistics.Snapshot> snapshot, Supplier<String> simulationType,
                      Supplier<SimulationState> simulationState, Actions actions) {
        this.target = target; this.controls = controls; this.statistics = statistics; this.chart = chart; this.log = log;
        this.nodeTable = nodeTable; this.connection = connection; this.snapshot = snapshot; this.simulationType = simulationType;
        this.simulationState = simulationState; this.actions = actions;
    }

    Node build() {
        VBox root = new VBox(12); root.getStyleClass().add("ddos-view");
        HBox title = new HBox(14, CyberIcon.of(CyberIcon.Type.NETWORK, 42, "ddos-title-icon"),
                ViewSupport.pageHeader("DDoS SIMULATION", "Simulate multiple logical traffic workers against the verified lab server"));
        title.setAlignment(Pos.CENTER_LEFT); title.getStyleClass().add("ddos-page-header");
        VBox targetBox = panel("TARGET SERVER", targetForm());
        VBox controlsBox = panel("SIMULATION CONTROL", controls);
        VBox leftColumn = new VBox(10, title, targetBox);
        leftColumn.getStyleClass().add("ddos-top-left-column");
        title.setMaxWidth(Double.MAX_VALUE);
        targetBox.setMaxWidth(Double.MAX_VALUE);

        HBox top = new HBox(12, leftColumn, controlsBox);
        top.setFillHeight(true);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(controlsBox, Priority.ALWAYS);
        leftColumn.setPrefWidth(620);
        controlsBox.setPrefWidth(520);
        controlsBox.setMaxHeight(Double.MAX_VALUE);

        VBox realtime = panel("REAL-TIME STATISTICS", statistics, chartBlock());
        VBox logPanel = logPanel();
        HBox middle = new HBox(12, realtime, logPanel);
        HBox.setHgrow(realtime, Priority.ALWAYS); HBox.setHgrow(logPanel, Priority.ALWAYS);
        realtime.setPrefWidth(680); logPanel.setPrefWidth(470);

        VBox nodes = panel("NODE STATUS", nodeTable);
        VBox result = resultPanel();
        VBox map = mapPanel();
        HBox bottom = new HBox(12, nodes, result, map);
        HBox.setHgrow(nodes, Priority.ALWAYS);
        HBox.setHgrow(result, Priority.ALWAYS);
        HBox.setHgrow(map, Priority.ALWAYS);
        nodes.setPrefWidth(480);
        result.setPrefWidth(350);
        map.setPrefWidth(300);
        root.getChildren().addAll(top, middle, bottom);
        return ViewSupport.page(root);
    }

    private Node targetForm() {
        TextField host = new TextField();
        TextField port = new TextField();
        host.textProperty().bindBidirectional(target.host.textProperty());
        port.textProperty().bindBidirectional(target.port.textProperty());
        ComboBox<String> protocol = new ComboBox<>();
        protocol.getItems().add("HTTP");
        protocol.setValue("HTTP");
        host.setMaxWidth(Double.MAX_VALUE);
        port.setMaxWidth(Double.MAX_VALUE);
        protocol.setMaxWidth(Double.MAX_VALUE);

        Button test = new Button("TEST CONNECTION", CyberIcon.of(CyberIcon.Type.LINK, 15, "button-icon"));
        test.getStyleClass().addAll("cyber-button", "ddos-test-connection");
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
        grid.setHgap(12);
        grid.setVgap(8);
        for (double width : new double[]{30, 20, 20, 30}) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        grid.add(new Label("Target IP"), 0, 0); grid.add(new Label("Port"), 1, 0); grid.add(new Label("Protocol"), 2, 0);
        grid.add(host, 0, 1); grid.add(port, 1, 1); grid.add(protocol, 2, 1);
        grid.add(test, 3, 0, 1, 2);
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

    private VBox chartBlock() {
        chart.setLegendVisible(false);
        Label chartTitle = new Label("Request Rate");
        chartTitle.getStyleClass().add("ddos-chart-title");
        Label unit = new Label("(requests/sec)");
        unit.getStyleClass().add("ddos-chart-unit");
        HBox title = new HBox(9, chartTitle, unit);
        title.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox legend = new HBox(18,
                legendItem("Total RPS", "ddos-legend-total"),
                legendItem("Successful RPS", "ddos-legend-success"));
        legend.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(10, title, spacer, legend);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox block = new VBox(6, header, chart);
        block.getStyleClass().add("ddos-chart-block");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return block;
    }

    private HBox legendItem(String text, String colorClass) {
        Region line = new Region();
        line.getStyleClass().addAll("ddos-legend-line", colorClass);
        Label label = new Label(text);
        label.getStyleClass().add("ddos-legend-label");
        HBox item = new HBox(7, line, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private VBox mapPanel() {
        ImageView map = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/client/assets/ddos-network-map.png"))));
        map.setPreserveRatio(false);
        map.setSmooth(true);
        map.setManaged(false);
        map.setLayoutX(2);
        map.setLayoutY(2);
        map.getStyleClass().add("ddos-map-image");

        FadeTransition mapPulse = new FadeTransition(Duration.seconds(1.15), map);
        mapPulse.setFromValue(.78);
        mapPulse.setToValue(.96);
        mapPulse.setAutoReverse(true);
        mapPulse.setCycleCount(Animation.INDEFINITE);
        mapPulse.play();

        AnimatedCodeBackdrop codeBackdrop = new AnimatedCodeBackdrop();
        AnimatedThreatOverlay threatOverlay = new AnimatedThreatOverlay();
        StackPane frame = new StackPane(codeBackdrop, map, threatOverlay);
        map.fitWidthProperty().bind(frame.widthProperty().subtract(4));
        map.fitHeightProperty().bind(frame.heightProperty().subtract(4));
        codeBackdrop.maxWidthProperty().bind(frame.widthProperty());
        codeBackdrop.maxHeightProperty().bind(frame.heightProperty());
        threatOverlay.maxWidthProperty().bind(frame.widthProperty());
        threatOverlay.maxHeightProperty().bind(frame.heightProperty());
        frame.setMinHeight(218);
        frame.setPrefHeight(218);
        frame.getStyleClass().add("ddos-map-frame");
        VBox panel = new VBox(frame);
        panel.getStyleClass().addAll("cyber-panel", "ddos-panel", "ddos-map-panel");
        panel.setPadding(new Insets(3));
        VBox.setVgrow(frame, Priority.ALWAYS);
        return panel;
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

    private VBox panel(String title, Node... children) { VBox panel = new VBox(10, sectionTitle(title)); panel.getChildren().addAll(children); panel.getStyleClass().addAll("cyber-panel", "ddos-panel"); panel.setPadding(new Insets(10)); return panel; }
    private Label sectionTitle(String text) {
        CyberIcon.Type icon = switch (text) {
            case "TARGET SERVER" -> CyberIcon.Type.SERVER;
            case "SIMULATION CONTROL" -> CyberIcon.Type.PLAY;
            case "REAL-TIME STATISTICS" -> CyberIcon.Type.CHART;
            case "SIMULATION LOG" -> CyberIcon.Type.LOG;
            case "NODE STATUS" -> CyberIcon.Type.NETWORK;
            case "SIMULATION RESULT" -> CyberIcon.Type.CLIPBOARD;
            default -> CyberIcon.Type.NETWORK;
        };
        Label label = new Label(text, CyberIcon.of(icon, 18, "ddos-section-icon"));
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("ddos-section-title");
        return label;
    }
}
