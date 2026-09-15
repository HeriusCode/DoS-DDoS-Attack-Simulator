package client.view;

import client.controller.AttackController;
import client.model.SimulationConfig;
import client.model.SimulationState;
import client.model.TargetServer;
import client.model.TrafficStatistics;
import client.network.ServerConnection;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class MainDashboard extends BorderPane {
    private final TargetPanel target = new TargetPanel();
    private final DoSSimulationPanel dos = new DoSSimulationPanel();
    private final DDoSSimulationPanel ddos = new DDoSSimulationPanel();
    private final StatisticsPanel statistics = new StatisticsPanel();
    private final DosStatisticsPanel dosStatistics = new DosStatisticsPanel();
    private final DdosStatisticsPanel ddosStatistics = new DdosStatisticsPanel();
    private final AttackController controller = new AttackController();
    private final TopNavigation tabs = new TopNavigation();

    private final TextArea dashboardLog = terminal();
    private final TextArea fullLog = terminal();
    private final TextArea connectionLog = terminal();
    private final TextArea dosLog = terminal();
    private final TextArea ddosLog = terminal();
    private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();
    private final FilteredList<LogEntry> filteredLogs = new FilteredList<>(logEntries, entry -> true);
    private final Label stateLabel = new Label("IDLE");
    private final Label typeLabel = new Label("-");
    private final Label clockLabel = new Label();
    private final Label dateLabel = new Label();
    private final Label connectionLabel = new Label("Not tested");
    private final Label responseTimeLabel = value("-");
    private final Label httpStatusLabel = value("-");
    private final Label lastCheckedLabel = value("-");
    private final Label summaryType = value("-");
    private final Label summaryTarget = value("127.0.0.1:8080");
    private final Label summaryRate = value("-");
    private final Label summaryNodes = value("-");
    private final Label summaryDuration = value("-");
    private final Label elapsedLabel = value("00:00");
    private final Label remainingLabel = value("--:--");
    private final ProgressIndicator progress = new ProgressIndicator(0);
    private final Label progressText = new Label("0%");
    private final Button dashboardStart = successButton("START");
    private final Button dashboardStop = dangerButton("STOP");
    private final XYChart.Series<Number, Number> rateSeries = new XYChart.Series<>();
    private final LineChart<Number, Number> rateChart = createChart();
    private final XYChart.Series<Number, Number> dosRateSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> dosTargetSeries = new XYChart.Series<>();
    private final LineChart<Number, Number> dosRateChart = createDosChart();
    private final XYChart.Series<Number, Number> ddosRateSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ddosSuccessSeries = new XYChart.Series<>();
    private final LineChart<Number, Number> ddosRateChart = createDdosChart();
    private final TableView<TrafficStatistics.WorkerSnapshot> nodeTable = createNodeTable();

    private volatile String verifiedTarget = "";
    private boolean selectedDdos;
    private int chartSample;

    public MainDashboard() {
        progressText.setId("progressText");
        getStyleClass().add("app-shell");
        setTop(buildHeader());
        tabs.configure(buildDashboard(), buildTargetView(), buildDosView(), buildDdosView(), buildLogsView(),
                ddosMode -> selectedDdos = ddosMode);
        VBox mainArea = new VBox(tabs);
        mainArea.setPadding(new Insets(4, 10, 10, 10));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        setCenter(mainArea);

        styleExistingButtons();

        target.test.setOnAction(event -> testConnection());
        dos.start.setOnAction(event -> startSimulation(false));
        ddos.start.setOnAction(event -> startSimulation(true));
        dos.stop.setOnAction(event -> stopSimulation());
        ddos.stop.setOnAction(event -> stopSimulation());
        dashboardStart.setOnAction(event -> startSimulation(selectedDdos));
        dashboardStop.setOnAction(event -> stopSimulation());

        Timeline refresh = new Timeline(new KeyFrame(Duration.millis(500), event -> refresh()));
        refresh.setCycleCount(Animation.INDEFINITE);
        refresh.play();
        updateControls();
        appendLog("INFO", "Application initialized");
        appendLog("INFO", "Ready. Verify a private lab target before starting.");
        appendConnectionLog("INFO", "Target module initialized");
        appendConnectionLog("INFO", "Ready for connection test");
    }

    private Node buildDashboard() {
        VBox content = new VBox(12);

        VBox statsAndChart = cyberPanel("REAL-TIME STATISTICS", statistics, sectionLabel("Request Rate  (requests/sec)"), rateChart);
        VBox logPanel = buildDashboardLogPanel();
        HBox middle = new HBox(12, statsAndChart, logPanel);
        HBox.setHgrow(statsAndChart, Priority.ALWAYS);
        HBox.setHgrow(logPanel, Priority.ALWAYS);
        statsAndChart.setPrefWidth(690);
        logPanel.setPrefWidth(500);

        VBox targetCard = buildDashboardTargetCard();
        VBox actions = buildQuickActions();
        VBox hacker = buildHackerCard();
        HBox bottom = new HBox(12, targetCard, actions, hacker);
        HBox.setHgrow(targetCard, Priority.ALWAYS);
        HBox.setHgrow(actions, Priority.ALWAYS);
        HBox.setHgrow(hacker, Priority.ALWAYS);
        targetCard.setPrefWidth(390);
        actions.setPrefWidth(310);
        hacker.setPrefWidth(360);

        content.getChildren().addAll(middle, bottom);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dashboard-scroll");
        return scroll;
    }

    private VBox buildDashboardLogPanel() {
        Button clear = smallButton("CLEAR");
        Button export = smallButton("EXPORT");
        clear.setOnAction(event -> {
            dashboardLog.clear();
            fullLog.clear();
            appendLog("INFO", "Log cleared");
        });
        export.setOnAction(event -> exportLog());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, sectionLabel("LOG"), spacer, clear, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(9, toolbar, dashboardLog);
        panel.getStyleClass().add("cyber-panel");
        panel.setPadding(new Insets(10));
        VBox.setVgrow(dashboardLog, Priority.ALWAYS);
        dashboardLog.setPrefHeight(360);
        return panel;
    }

    private VBox buildDashboardTargetCard() {
        TextField host = new TextField();
        TextField port = new TextField();
        host.textProperty().bindBidirectional(target.host.textProperty());
        port.textProperty().bindBidirectional(target.port.textProperty());
        host.getStyleClass().add("cyber-field");
        port.getStyleClass().add("cyber-field");
        ComboBox<String> protocol = new ComboBox<>();
        protocol.getItems().add("HTTP");
        protocol.setValue("HTTP");
        protocol.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(9);
        form.add(new Label("IP Address"), 0, 0); form.add(host, 1, 0);
        form.add(new Label("Port"), 0, 1); form.add(port, 1, 1);
        form.add(new Label("Protocol"), 0, 2); form.add(protocol, 1, 2);
        ColumnConstraints labels = new ColumnConstraints(85);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);

        Button test = successButton("TEST CONNECTION");
        test.setMaxWidth(Double.MAX_VALUE);
        test.setOnAction(event -> testConnection());
        Label dashboardConnection = new Label();
        dashboardConnection.getStyleClass().add("status-connected");
        Timeline statusRefresh = new Timeline(new KeyFrame(Duration.millis(500), event ->
                dashboardConnection.setText(connectionLabel.getText())));
        statusRefresh.setCycleCount(Animation.INDEFINITE);
        statusRefresh.play();
        return cyberPanel("TARGET SERVER CONFIGURATION", form, test, dashboardConnection);
    }

    private VBox buildQuickActions() {
        dashboardStart.setMaxWidth(Double.MAX_VALUE);
        dashboardStop.setMaxWidth(Double.MAX_VALUE);
        Button reset = secondaryButton("RESET");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(event -> resetVisuals());
        return cyberPanel("QUICK ACTIONS", dashboardStart, dashboardStop, reset);
    }

    private VBox buildHackerCard() {
        ImageView image = new ImageView(new Image(
                MainDashboard.class.getResourceAsStream("/client/assets/hacker-quick-actions.png")));
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.setFitWidth(315);
        image.setFitHeight(220);
        StackPane frame = new StackPane(image);
        frame.getStyleClass().add("hacker-frame");
        frame.setMinHeight(215);
        VBox box = new VBox(frame);
        box.getStyleClass().add("hacker-card");
        VBox.setVgrow(frame, Priority.ALWAYS);
        return box;
    }

    private Node buildTargetView() {
        VBox root = new VBox(14, pageHeader("TARGET SERVER", "Configure the target server (IP, Port, Protocol) and test connection"));
        VBox configuration = cyberPanel("SERVER CONFIGURATION", target);
        VBox right = new VBox(12, buildServerInformationPanel(), buildQuickPresets(), buildConnectionLogPanel());
        VBox.setVgrow(right.getChildren().get(2), Priority.ALWAYS);
        HBox body = new HBox(14, configuration, right);
        HBox.setHgrow(configuration, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        configuration.setPrefWidth(620);
        right.setPrefWidth(520);
        root.getChildren().add(body);
        return page(root);
    }

    private Node buildDosView() {
        VBox root = new VBox(14);
        root.getStyleClass().add("dos-view");

        HBox title = new HBox(14,
                CyberIcon.of(CyberIcon.Type.TARGET, 42, "dos-title-icon"),
                pageHeader("DoS SIMULATION", "Simulate controlled single-source traffic against the verified lab server"));
        title.setAlignment(Pos.CENTER_LEFT);
        title.getStyleClass().add("dos-page-header");

        VBox targetBox = dosPanel("TARGET SERVER", buildDosTargetForm());
        VBox controlsBox = dosPanel("SIMULATION CONTROLS", dos);
        HBox top = new HBox(14, targetBox, controlsBox);
        HBox.setHgrow(targetBox, Priority.ALWAYS);
        HBox.setHgrow(controlsBox, Priority.ALWAYS);
        targetBox.setPrefWidth(640);
        controlsBox.setPrefWidth(500);

        VBox realtime = dosPanel("REAL-TIME STATISTICS", dosStatistics, dosRateChart);
        VBox runtimeLog = buildDosLogPanel();
        HBox middle = new HBox(14, realtime, runtimeLog);
        HBox.setHgrow(realtime, Priority.ALWAYS);
        HBox.setHgrow(runtimeLog, Priority.ALWAYS);
        realtime.setPrefWidth(680);
        runtimeLog.setPrefWidth(470);

        root.getChildren().addAll(title, top, middle, buildDosResultPanel());
        return page(root);
    }

    private Node buildDosTargetForm() {
        TextField host = new TextField();
        TextField port = new TextField();
        host.textProperty().bindBidirectional(target.host.textProperty());
        port.textProperty().bindBidirectional(target.port.textProperty());
        ComboBox<String> protocol = new ComboBox<>();
        protocol.getItems().add("HTTP");
        protocol.setValue("HTTP");
        protocol.setMaxWidth(Double.MAX_VALUE);
        host.setMaxWidth(Double.MAX_VALUE);
        port.setMaxWidth(Double.MAX_VALUE);

        Label connection = new Label();
        connection.getStyleClass().add("dos-connection");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event ->
                connection.setText(connectionLabel.getText())));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();

        Button test = secondaryButton("TEST CONNECTION");
        test.setOnAction(event -> testConnection());
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(20);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(20);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(30);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);
        grid.add(new Label("Target IP"), 0, 0); grid.add(new Label("Port"), 1, 0); grid.add(new Label("Protocol"), 2, 0);
        grid.add(host, 0, 1); grid.add(port, 1, 1); grid.add(protocol, 2, 1);
        VBox connectionBox = new VBox(3, connection, new Label("Server reachability"));
        connectionBox.getStyleClass().add("dos-connection-box");
        grid.add(connectionBox, 3, 0, 1, 2);
        grid.add(test, 3, 2);
        return grid;
    }

    private VBox buildDosLogPanel() {
        Button clear = smallButton("CLEAR");
        Button export = smallButton("EXPORT");
        clear.setOnAction(event -> dosLog.clear());
        export.setOnAction(event -> exportLog());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, dosSectionTitle("LOG"), spacer, clear, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        dosLog.setPrefHeight(340);
        VBox panel = new VBox(9, toolbar, dosLog);
        panel.getStyleClass().addAll("cyber-panel", "dos-panel");
        panel.setPadding(new Insets(10));
        VBox.setVgrow(dosLog, Priority.ALWAYS);
        return panel;
    }

    private VBox buildDosResultPanel() {
        Label type = value("DoS");
        Label resultTarget = value("-");
        Label resultDuration = value("-");
        Label resultSent = value("0");
        Label resultSuccess = value("0");
        Label resultFailed = value("0");
        Label resultLimited = value("0");
        Label resultAverage = value("0 ms");
        Label resultRps = value("0");
        Label notice = new Label("No DoS simulation data yet");
        notice.getStyleClass().add("dos-result-notice");

        GridPane result = new GridPane();
        result.setHgap(20); result.setVgap(7);
        addResultCell(result, 0, 0, "Attack Type", type);
        addResultCell(result, 0, 1, "Target", resultTarget);
        addResultCell(result, 0, 2, "Duration", resultDuration);
        addResultCell(result, 1, 0, "Requests Sent", resultSent);
        addResultCell(result, 1, 1, "Successful", resultSuccess);
        addResultCell(result, 1, 2, "Failed", resultFailed);
        addResultCell(result, 2, 0, "Limited / Rejected", resultLimited);
        addResultCell(result, 2, 1, "Average Response", resultAverage);
        addResultCell(result, 2, 2, "Average Requests/sec", resultRps);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.333); column.setHgrow(Priority.ALWAYS);
            result.getColumnConstraints().add(column);
        }

        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            var snapshot = controller.snapshot();
            boolean isDos = "DoS".equals(typeLabel.getText());
            if (snapshot == null || !isDos) return;
            resultTarget.setText(target.host.getText() + ":" + target.port.getText());
            resultDuration.setText((snapshot.elapsedSeconds() + snapshot.remainingSeconds()) + " seconds");
            resultSent.setText(Long.toString(snapshot.sent()));
            resultSuccess.setText(Long.toString(snapshot.successful()));
            resultFailed.setText(Long.toString(snapshot.failed()));
            resultLimited.setText(Long.toString(snapshot.limited()));
            resultAverage.setText(String.format("%.1f ms", snapshot.averageResponseMs()));
            resultRps.setText(String.format("%.1f", snapshot.currentRate()));
            notice.setText(controller.state() == SimulationState.RUNNING ? "DoS simulation is running" : "DoS simulation result available");
        }));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();

        HBox body = new HBox(18, result, notice);
        HBox.setHgrow(result, Priority.ALWAYS);
        return dosPanel("SIMULATION RESULT", body);
    }

    private VBox dosPanel(String title, Node... children) {
        VBox panel = new VBox(11, dosSectionTitle(title));
        panel.getChildren().addAll(children);
        panel.getStyleClass().addAll("cyber-panel", "dos-panel");
        panel.setPadding(new Insets(11));
        return panel;
    }

    private Label dosSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dos-section-title");
        return label;
    }

    private void addResultCell(GridPane grid, int column, int row, String name, Label value) {
        grid.add(new VBox(2, new Label(name), value), column, row);
    }

    private Node buildDdosView() {
        VBox root = new VBox(12);
        root.getStyleClass().add("ddos-view");
        HBox title = new HBox(14,
                CyberIcon.of(CyberIcon.Type.NETWORK, 42, "ddos-title-icon"),
                pageHeader("DDoS SIMULATION", "Simulate multiple logical traffic workers against the verified lab server"));
        title.setAlignment(Pos.CENTER_LEFT);
        title.getStyleClass().add("ddos-page-header");

        VBox targetBox = ddosPanel("TARGET CONFIGURATION", buildDdosTargetForm());
        VBox controlsBox = ddosPanel("SIMULATION CONTROL", ddos);
        HBox top = new HBox(12, targetBox, controlsBox);
        HBox.setHgrow(targetBox, Priority.ALWAYS); HBox.setHgrow(controlsBox, Priority.ALWAYS);
        targetBox.setPrefWidth(620); controlsBox.setPrefWidth(520);

        VBox realtime = ddosPanel("REAL-TIME STATISTICS", ddosStatistics, ddosRateChart);
        VBox logPanel = buildDdosLogPanel();
        HBox middle = new HBox(12, realtime, logPanel);
        HBox.setHgrow(realtime, Priority.ALWAYS); HBox.setHgrow(logPanel, Priority.ALWAYS);
        realtime.setPrefWidth(680); logPanel.setPrefWidth(470);

        VBox nodes = ddosPanel("NODE STATUS", nodeTable);
        VBox result = buildDdosResultPanel();
        VBox actions = buildDdosQuickActions();
        HBox bottom = new HBox(12, nodes, result, actions);
        HBox.setHgrow(nodes, Priority.ALWAYS); HBox.setHgrow(result, Priority.ALWAYS); HBox.setHgrow(actions, Priority.ALWAYS);
        nodes.setPrefWidth(480); result.setPrefWidth(350); actions.setPrefWidth(300);

        root.getChildren().addAll(title, top, middle, bottom);
        return page(root);
    }

    private Node buildDdosTargetForm() {
        TextField host = new TextField();
        TextField port = new TextField();
        host.textProperty().bindBidirectional(target.host.textProperty());
        port.textProperty().bindBidirectional(target.port.textProperty());
        ComboBox<String> protocol = new ComboBox<>();
        protocol.getItems().add("HTTP"); protocol.setValue("HTTP");
        host.setMaxWidth(Double.MAX_VALUE); port.setMaxWidth(Double.MAX_VALUE); protocol.setMaxWidth(Double.MAX_VALUE);
        Button test = successButton("TEST CONNECTION");
        test.setOnAction(event -> testConnection());
        Label connected = new Label();
        connected.getStyleClass().add("ddos-connection");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> connected.setText(connectionLabel.getText())));
        update.setCycleCount(Animation.INDEFINITE); update.play();

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(7);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25); column.setHgrow(Priority.ALWAYS); grid.getColumnConstraints().add(column);
        }
        grid.add(new Label("Target IP"),0,0); grid.add(new Label("Port"),1,0); grid.add(new Label("Protocol"),2,0);
        grid.add(host,0,1); grid.add(port,1,1); grid.add(protocol,2,1); grid.add(test,3,1); grid.add(connected,3,2);
        return grid;
    }

    private VBox buildDdosLogPanel() {
        Button clear = smallButton("CLEAR"); Button export = smallButton("EXPORT");
        clear.setOnAction(event -> ddosLog.clear()); export.setOnAction(event -> exportLog());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(8, ddosSectionTitle("SIMULATION LOG"), spacer, clear, export);
        tools.setAlignment(Pos.CENTER_LEFT);
        ddosLog.setPrefHeight(340);
        VBox panel = new VBox(9, tools, ddosLog);
        panel.getStyleClass().addAll("cyber-panel", "ddos-panel"); panel.setPadding(new Insets(10));
        VBox.setVgrow(ddosLog, Priority.ALWAYS);
        return panel;
    }

    private VBox buildDdosResultPanel() {
        Label attack = value("DDoS"), resultTarget = value("-"), resultNodes = value("-"),
                resultRate = value("-"), resultDuration = value("-");
        Label message = new Label("No DDoS simulation data yet");
        message.getStyleClass().add("ddos-result-message");
        GridPane grid = new GridPane(); grid.setHgap(14); grid.setVgap(7);
        addInfoRow(grid,0,"Attack Type",attack); addInfoRow(grid,1,"Target",resultTarget);
        addInfoRow(grid,2,"Simulated Nodes",resultNodes); addInfoRow(grid,3,"Requests/sec/node",resultRate);
        addInfoRow(grid,4,"Duration",resultDuration);
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            var snapshot = controller.snapshot();
            if (snapshot == null || !"DDoS".equals(typeLabel.getText())) return;
            resultTarget.setText(target.host.getText()+":"+target.port.getText());
            resultNodes.setText(ddos.nodes.getText()); resultRate.setText(ddos.rate.getText());
            resultDuration.setText((snapshot.elapsedSeconds()+snapshot.remainingSeconds())+" seconds");
            message.setText(controller.state()==SimulationState.RUNNING ? "DDoS simulation is running" : "Simulation completed");
        }));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        return ddosPanel("SIMULATION RESULT", grid, message);
    }

    private VBox buildDdosQuickActions() {
        Button start = successButton("START"); Button stop = dangerButton("STOP"); Button reset = secondaryButton("RESET");
        start.setMaxWidth(Double.MAX_VALUE); stop.setMaxWidth(Double.MAX_VALUE); reset.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(event -> startSimulation(true)); stop.setOnAction(event -> stopSimulation()); reset.setOnAction(event -> resetVisuals());
        Timeline enabled = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            boolean running = controller.state()==SimulationState.RUNNING;
            start.setDisable(running || verifiedTarget.isBlank()); stop.setDisable(!running);
        }));
        enabled.setCycleCount(Animation.INDEFINITE); enabled.play();
        Label hint = new Label("Verify the private lab target before starting any logical workers.");
        hint.setWrapText(true); hint.getStyleClass().add("hint-box");
        return ddosPanel("QUICK ACTIONS", start, stop, reset, hint);
    }

    private VBox ddosPanel(String title, Node... children) {
        VBox panel = new VBox(10, ddosSectionTitle(title)); panel.getChildren().addAll(children);
        panel.getStyleClass().addAll("cyber-panel", "ddos-panel"); panel.setPadding(new Insets(10)); return panel;
    }

    private Label ddosSectionTitle(String text) {
        Label label = new Label(text); label.getStyleClass().add("ddos-section-title"); return label;
    }

    private VBox buildServerInformationPanel() {
        Label ip = value("");
        Label port = value("");
        Label status = value("");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            ip.setText(target.host.getText());
            port.setText(target.port.getText());
            status.setText(connectionLabel.getText());
        }));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();

        GridPane details = new GridPane();
        details.setHgap(18); details.setVgap(9);
        addInfoRow(details, 0, "Target IP", ip);
        addInfoRow(details, 1, "Port", port);
        addInfoRow(details, 2, "Protocol", value("HTTP"));
        addInfoRow(details, 3, "Status", status);
        addInfoRow(details, 4, "Response Time", responseTimeLabel);
        addInfoRow(details, 5, "HTTP Status", httpStatusLabel);
        addInfoRow(details, 6, "Last Checked", lastCheckedLabel);

        StackPane serverVisual = new StackPane(CyberIcon.of(CyberIcon.Type.SERVER, 92, "server-large-icon"));
        serverVisual.getStyleClass().add("server-visual");
        HBox body = new HBox(22, details, serverVisual);
        HBox.setHgrow(details, Priority.ALWAYS);
        body.setAlignment(Pos.CENTER_LEFT);
        return cyberPanel("SERVER INFORMATION", body);
    }

    private VBox buildQuickPresets() {
        Button local = presetButton("LOCAL TEST", "127.0.0.1:8080 (HTTP)", "127.0.0.1", "8080");
        Button lab = presetButton("LAB SERVER", "192.168.1.20:8080 (HTTP)", "192.168.1.20", "8080");
        Label note = new Label("Presets only fill the form. Press Test Connection to verify.");
        note.getStyleClass().add("form-hint");
        return cyberPanel("QUICK PRESETS", local, lab, note);
    }

    private Button presetButton(String title, String address, String host, String port) {
        Label name = new Label(title);
        name.getStyleClass().add("preset-name");
        Label value = new Label(address);
        value.getStyleClass().add("preset-address");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, CyberIcon.of(CyberIcon.Type.SERVER, 22, "preset-icon"), name, spacer, value, new Label(">"));
        row.setAlignment(Pos.CENTER_LEFT);
        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("preset-button");
        button.setOnAction(event -> {
            target.host.setText(host);
            target.port.setText(port);
            verifiedTarget = "";
            connectionLabel.setText("Not tested");
            target.status.setText("NOT TESTED");
            appendConnectionLog("INFO", "Preset selected: " + address + ". Connection test required.");
            updateControls();
        });
        return button;
    }

    private VBox buildConnectionLogPanel() {
        Button clear = smallButton("CLEAR");
        clear.setOnAction(event -> connectionLog.clear());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, sectionLabel("RECENT CONNECTION LOGS"), spacer, clear);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        connectionLog.setPrefHeight(190);
        VBox box = new VBox(9, toolbar, connectionLog);
        box.getStyleClass().add("cyber-panel");
        box.setPadding(new Insets(10));
        VBox.setVgrow(connectionLog, Priority.ALWAYS);
        return box;
    }

    private Node buildLogsView() {
        TableView<LogEntry> table = createLogTable();
        table.setItems(filteredLogs);

        ComboBox<String> filter = new ComboBox<>();
        filter.getItems().addAll("ALL", "INFO", "TRAFFIC", "WARNING", "ERROR", "DETECTION", "DEFENSE");
        filter.setValue("ALL");
        ComboBox<String> timeRange = new ComboBox<>();
        timeRange.getItems().addAll("Current session", "Last 1 hour");
        timeRange.setValue("Current session");
        TextField search = new TextField();
        search.setPromptText("Enter keyword...");
        search.textProperty().addListener((observable, oldValue, newValue) -> applyLogFilter(filter.getValue(), newValue));
        filter.valueProperty().addListener((observable, oldValue, newValue) -> applyLogFilter(newValue, search.getText()));

        Button clear = secondaryButton("CLEAR");
        Button export = secondaryButton("EXPORT");
        clear.setOnAction(event -> clearAllLogs());
        export.setOnAction(event -> exportLog());

        HBox levelButtons = new HBox(9,
                categoryButton("ALL", "all", filter), categoryButton("INFO", "info", filter),
                categoryButton("TRAFFIC", "traffic", filter), categoryButton("WARNING", "warning", filter),
                categoryButton("ERROR", "error", filter), categoryButton("DETECTION", "detection", filter),
                categoryButton("DEFENSE", "defense", filter));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(8, levelButtons, spacer, clear, export);
        tools.setAlignment(Pos.CENTER_LEFT);

        VBox filterPanel = cyberPanel("LOG FILTER",
                new Label("LOG TYPE"), filter,
                new Label("TIME RANGE"), timeRange,
                new Label("SEARCH"), search);
        filter.setMaxWidth(Double.MAX_VALUE); timeRange.setMaxWidth(Double.MAX_VALUE); search.setMaxWidth(Double.MAX_VALUE);

        Label total = value("0"), info = value("0"), traffic = value("0"), warning = value("0"), error = value("0"), detection = value("0"), defense = value("0");
        GridPane quickGrid = new GridPane(); quickGrid.setHgap(18); quickGrid.setVgap(8);
        addInfoRow(quickGrid,0,"Total Logs",total); addInfoRow(quickGrid,1,"Info",info);
        addInfoRow(quickGrid,2,"Traffic",traffic); addInfoRow(quickGrid,3,"Warning",warning);
        addInfoRow(quickGrid,4,"Error",error); addInfoRow(quickGrid,5,"Detection",detection); addInfoRow(quickGrid,6,"Defense",defense);
        VBox quickStats = cyberPanel("QUICK STATS", quickGrid);

        CheckBox autoScroll = new CheckBox("Auto scroll enabled");
        autoScroll.setSelected(true);
        VBox live = cyberPanel("LIVE LOGS", autoScroll);
        logEntries.addListener((javafx.collections.ListChangeListener<LogEntry>) change -> {
            if (autoScroll.isSelected() && !filteredLogs.isEmpty()) Platform.runLater(() -> table.scrollTo(filteredLogs.size()-1));
        });
        Timeline counts = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            total.setText(Integer.toString(logEntries.size())); info.setText(Long.toString(countLevel("INFO")));
            traffic.setText(Long.toString(countLevel("TRAFFIC"))); warning.setText(Long.toString(countLevel("WARNING")));
            error.setText(Long.toString(countLevel("ERROR"))); detection.setText(Long.toString(countLevel("DETECTION")));
            defense.setText(Long.toString(countLevel("DEFENSE")));
        }));
        counts.setCycleCount(Animation.INDEFINITE); counts.play();

        VBox tablePanel = cyberPanel("EVENT STREAM", table);
        VBox.setVgrow(table, Priority.ALWAYS); table.setPrefHeight(510);
        VBox right = new VBox(10, filterPanel, quickStats, live); right.setPrefWidth(280);
        HBox body = new HBox(12, tablePanel, right);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        VBox root = new VBox(12, pageHeader("SYSTEM LOGS", "View and monitor all system events, attacks and simulation logs"), tools, body);
        return page(root);
    }

    private HBox buildHeader() {
        ImageView logoImage = new ImageView(new Image(
                MainDashboard.class.getResourceAsStream("/client/assets/header-logo.png")));
        logoImage.setPreserveRatio(true);
        logoImage.setSmooth(true);
        logoImage.setFitWidth(39);
        logoImage.setFitHeight(39);
        StackPane logo = new StackPane(logoImage);
        logo.getStyleClass().add("header-logo");
        Label brand = new Label("DOS/DDoS ATTACK SIMULATOR");
        brand.getStyleClass().add("brand");
        Label environment = new Label("|   Lab Environment   |   Educational Use Only");
        environment.getStyleClass().add("safe");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ready = new Label("●  SYSTEM READY");
        ready.getStyleClass().add("system-ready");
        Label headerClock = new Label();
        headerClock.getStyleClass().add("header-clock");
        Timeline time = new Timeline(new KeyFrame(Duration.seconds(1), event ->
                headerClock.setText(LocalDate.now() + "  " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        time.setCycleCount(Animation.INDEFINITE);
        time.play();
        HBox header = new HBox(16, logo, brand, environment, spacer, ready, headerClock);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    private void testConnection() {
        final TargetServer requested;
        final long startedAt = System.nanoTime();
        try {
            requested = targetServer();
            connectionLabel.setText("● Testing...");
            appendConnectionLog("INFO", "Testing connection to " + requested.uri());
        } catch (Exception exception) {
            showError(exception.getMessage());
            return;
        }
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() { return new ServerConnection().test(requested); }
        };
        task.setOnSucceeded(event -> {
            boolean connected = task.getValue();
            verifiedTarget = connected ? targetKey(requested) : "";
            long responseMs = (System.nanoTime() - startedAt) / 1_000_000;
            connectionLabel.setText(connected ? "● Connected" : "● Connection Failed");
            target.status.setText(connectionLabel.getText());
            responseTimeLabel.setText(responseMs + " ms");
            httpStatusLabel.setText(connected ? "Reachable" : "Unavailable");
            lastCheckedLabel.setText(LocalDate.now() + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            appendLog(connected ? "INFO" : "ERROR", connected
                    ? "Connection test successful: " + requested.uri()
                    : "Connection test failed: " + requested.uri());
            appendConnectionLog(connected ? "INFO" : "ERROR", connected
                    ? "Connection successful in " + responseMs + " ms"
                    : "Connection failed after " + responseMs + " ms");
            updateControls();
        });
        task.setOnFailed(event -> {
            verifiedTarget = "";
            connectionLabel.setText("● Connection Failed");
            target.status.setText(connectionLabel.getText());
            responseTimeLabel.setText("-");
            httpStatusLabel.setText("Error");
            lastCheckedLabel.setText(LocalDate.now() + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            appendLog("ERROR", "Connection test error: " + task.getException().getMessage());
            appendConnectionLog("ERROR", task.getException().getMessage());
            updateControls();
        });
        Thread thread = new Thread(task, "lab-connection-test");
        thread.setDaemon(true);
        thread.start();
    }

    private void startSimulation(boolean ddosMode) {
        try {
            TargetServer server = targetServer();
            if (!targetKey(server).equals(verifiedTarget)) throw new IllegalArgumentException("Test this exact lab target before starting");
            int nodes = ddosMode ? Integer.parseInt(ddos.nodes.getText()) : 1;
            int rate = Integer.parseInt((ddosMode ? ddos.rate : dos.rate).getText());
            int duration = Integer.parseInt((ddosMode ? ddos.duration : dos.duration).getText());
            SimulationConfig config = new SimulationConfig(nodes, rate, duration);
            selectedDdos = ddosMode;
            chartSample = 0;
            rateSeries.getData().clear();
            if (!ddosMode) {
                dosRateSeries.getData().clear();
                dosTargetSeries.getData().clear();
            } else {
                ddosRateSeries.getData().clear();
                ddosSuccessSeries.getData().clear();
                nodeTable.getItems().clear();
            }
            controller.start(server, config, ddosMode, () -> Platform.runLater(() -> {
                stateLabel.setText("COMPLETED");
                dos.state.setText(ddosMode ? "STOPPED" : "COMPLETED");
                ddos.state.setText(ddosMode ? "COMPLETED" : "STOPPED");
                appendLog("INFO", "Simulation completed");
                updateControls();
            }));
            typeLabel.setText(ddosMode ? "DDoS" : "DoS");
            stateLabel.setText("RUNNING");
            dos.state.setText(ddosMode ? "STOPPED" : "RUNNING");
            ddos.state.setText(ddosMode ? "RUNNING" : "STOPPED");
            summaryType.setText(typeLabel.getText());
            summaryTarget.setText(targetKey(server));
            summaryRate.setText(rate + (ddosMode ? " / node" : " req/s"));
            summaryNodes.setText(ddosMode ? Integer.toString(nodes) : "-");
            summaryDuration.setText(duration + " seconds");
            appendLog("INFO", typeLabel.getText() + " simulation started");
            appendLog("TRAFFIC", "Rate: " + rate + " req/s, duration: " + duration + " seconds");
            updateControls();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void stopSimulation() {
        controller.stop();
        stateLabel.setText("STOPPED");
        dos.state.setText("STOPPED");
        ddos.state.setText("STOPPED");
        appendLog("INFO", "Simulation stopped; workers released");
        updateControls();
    }

    private void refresh() {
        clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        dateLabel.setText(LocalDate.now().toString());
        var snapshot = controller.snapshot();
        statistics.show(snapshot);
        dosStatistics.show("DoS".equals(typeLabel.getText()) ? snapshot : null);
        int configuredNodes;
        try { configuredNodes = Integer.parseInt(ddos.nodes.getText()); } catch (NumberFormatException exception) { configuredNodes = 0; }
        boolean isDdos = "DDoS".equals(typeLabel.getText());
        ddosStatistics.show(isDdos ? snapshot : null, configuredNodes);
        if (snapshot != null) {
            rateSeries.getData().add(new XYChart.Data<>(chartSample++, snapshot.currentRate()));
            while (rateSeries.getData().size() > 31) rateSeries.getData().remove(0);
            if ("DoS".equals(typeLabel.getText())) {
                int targetRate;
                try { targetRate = Integer.parseInt(dos.rate.getText()); } catch (NumberFormatException exception) { targetRate = 0; }
                dosRateSeries.getData().add(new XYChart.Data<>(chartSample, snapshot.currentRate()));
                dosTargetSeries.getData().add(new XYChart.Data<>(chartSample, targetRate));
                while (dosRateSeries.getData().size() > 31) dosRateSeries.getData().remove(0);
                while (dosTargetSeries.getData().size() > 31) dosTargetSeries.getData().remove(0);
            }
            if (isDdos) {
                double successfulRate = snapshot.elapsedSeconds()==0 ? 0 : snapshot.successful()/(double)snapshot.elapsedSeconds();
                ddosRateSeries.getData().add(new XYChart.Data<>(chartSample,snapshot.currentRate()));
                ddosSuccessSeries.getData().add(new XYChart.Data<>(chartSample,successfulRate));
                while(ddosRateSeries.getData().size()>31) ddosRateSeries.getData().remove(0);
                while(ddosSuccessSeries.getData().size()>31) ddosSuccessSeries.getData().remove(0);
                nodeTable.getItems().setAll(snapshot.workers());
            }
            elapsedLabel.setText(formatTime(snapshot.elapsedSeconds()));
            remainingLabel.setText(formatTime(snapshot.remainingSeconds()));
            long total = snapshot.elapsedSeconds() + snapshot.remainingSeconds();
            double fraction = total == 0 ? 0 : Math.min(1, snapshot.elapsedSeconds() / (double) total);
            progress.setProgress(fraction);
            progressText.setText(Math.round(fraction * 100) + "%");
            if (controller.state() != SimulationState.RUNNING && "RUNNING".equals(stateLabel.getText())) stateLabel.setText(controller.state().name());
        }
        updateControls();
    }

    private void updateControls() {
        boolean running = controller.state() == SimulationState.RUNNING;
        boolean verified = !verifiedTarget.isBlank();
        dashboardStart.setDisable(running || !verified);
        dashboardStop.setDisable(!running);
        dos.start.setDisable(running || !verified);
        ddos.start.setDisable(running || !verified);
        dos.stop.setDisable(!running);
        ddos.stop.setDisable(!running);
    }

    private void resetVisuals() {
        if (controller.state() == SimulationState.RUNNING) return;
        rateSeries.getData().clear(); chartSample = 0;
        dosRateSeries.getData().clear(); dosTargetSeries.getData().clear();
        ddosRateSeries.getData().clear(); ddosSuccessSeries.getData().clear(); nodeTable.getItems().clear();
        progress.setProgress(0); progressText.setText("0%");
        elapsedLabel.setText("00:00"); remainingLabel.setText("--:--");
        summaryType.setText("-"); summaryRate.setText("-"); summaryNodes.setText("-"); summaryDuration.setText("-");
        stateLabel.setText("IDLE"); typeLabel.setText("-"); dos.state.setText("STOPPED"); ddos.state.setText("STOPPED"); statistics.show(null);
        appendLog("INFO", "Dashboard display reset");
    }

    private void exportLog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export simulation log");
        chooser.setInitialFileName("simulation-log.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), fullLog.getText());
            appendLog("INFO", "Log exported to " + file.getName());
        } catch (IOException exception) {
            showError("Cannot export log: " + exception.getMessage());
        }
    }

    private void appendLog(String level, String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String line = "[" + time + "] [" + level + "] " + message + System.lineSeparator();
        dashboardLog.appendText(line);
        fullLog.appendText(line);
        dosLog.appendText(line);
        ddosLog.appendText(line);
        logEntries.add(new LogEntry("[" + time + "]", level, componentFor(message), message));
    }

    private void appendConnectionLog(String level, String message) {
        connectionLog.appendText("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] [" + level + "] " + message + System.lineSeparator());
    }

    private void showError(String message) {
        appendLog("ERROR", message);
        new Alert(Alert.AlertType.ERROR, message).show();
    }

    private TargetServer targetServer() {
        return new TargetServer(target.host.getText().trim(), Integer.parseInt(target.port.getText().trim()));
    }

    private String targetKey(TargetServer server) { return server.host() + ":" + server.port(); }
    private String formatTime(long seconds) { return "%02d:%02d".formatted(seconds / 60, seconds % 60); }

    private LineChart<Number, Number> createChart() {
        NumberAxis x = new NumberAxis(0, 30, 5);
        NumberAxis y = new NumberAxis(0, SimulationConfig.MAX_NODES * SimulationConfig.MAX_RATE_PER_NODE, 10);
        x.setLabel("Time"); y.setLabel("Requests/sec");
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false); chart.setCreateSymbols(false); chart.setLegendVisible(false);
        chart.setMinHeight(180); chart.setPrefHeight(205);
        rateSeries.setName("Request Rate");
        chart.getData().add(rateSeries);
        return chart;
    }

    private LineChart<Number, Number> createDosChart() {
        NumberAxis x = new NumberAxis();
        NumberAxis y = new NumberAxis();
        x.setLabel("Time");
        y.setLabel("Requests/sec");
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(true);
        chart.setMinHeight(190);
        chart.setPrefHeight(220);
        dosRateSeries.setName("Current RPS");
        dosTargetSeries.setName("Target RPS");
        chart.getData().add(dosRateSeries);
        chart.getData().add(dosTargetSeries);
        chart.getStyleClass().add("dos-chart");
        return chart;
    }

    private LineChart<Number, Number> createDdosChart() {
        NumberAxis x = new NumberAxis(); NumberAxis y = new NumberAxis();
        x.setLabel("Time"); y.setLabel("Requests/sec");
        LineChart<Number, Number> chart = new LineChart<>(x,y);
        chart.setAnimated(false); chart.setCreateSymbols(false); chart.setLegendVisible(true);
        chart.setMinHeight(185); chart.setPrefHeight(215);
        ddosRateSeries.setName("Total RPS"); ddosSuccessSeries.setName("Successful RPS");
        chart.getData().add(ddosRateSeries);
        chart.getData().add(ddosSuccessSeries);
        chart.getStyleClass().add("ddos-chart"); return chart;
    }

    private TableView<TrafficStatistics.WorkerSnapshot> createNodeTable() {
        TableView<TrafficStatistics.WorkerSnapshot> table = new TableView<>();
        table.getStyleClass().add("node-table"); table.setPrefHeight(190);
        TableColumn<TrafficStatistics.WorkerSnapshot,String> id = nodeColumn("Node ID", row -> "Node " + row.workerId());
        TableColumn<TrafficStatistics.WorkerSnapshot,String> status = nodeColumn("Status", row -> row.active() ? "● Running" : "● Stopped");
        TableColumn<TrafficStatistics.WorkerSnapshot,String> sent = nodeColumn("Requests Sent", row -> Long.toString(row.sent()));
        TableColumn<TrafficStatistics.WorkerSnapshot,String> success = nodeColumn("Success", row -> Long.toString(row.successful()));
        TableColumn<TrafficStatistics.WorkerSnapshot,String> failed = nodeColumn("Failed", row -> Long.toString(row.failed()));
        TableColumn<TrafficStatistics.WorkerSnapshot,String> rps = nodeColumn("RPS", row -> String.format("%.1f",row.requestsPerSecond()));
        id.setPrefWidth(75); status.setPrefWidth(95); sent.setPrefWidth(105); success.setPrefWidth(75); failed.setPrefWidth(65); rps.setPrefWidth(60);
        table.getColumns().add(id); table.getColumns().add(status); table.getColumns().add(sent);
        table.getColumns().add(success); table.getColumns().add(failed); table.getColumns().add(rps);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Start DDoS to initialize logical workers"));
        return table;
    }

    private TableColumn<TrafficStatistics.WorkerSnapshot,String> nodeColumn(
            String title, java.util.function.Function<TrafficStatistics.WorkerSnapshot,String> mapper) {
        TableColumn<TrafficStatistics.WorkerSnapshot,String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(mapper.apply(cell.getValue())));
        return column;
    }

    private TableView<LogEntry> createLogTable() {
        TableView<LogEntry> table = new TableView<>();
        table.getStyleClass().add("system-log-table");
        TableColumn<LogEntry,String> time = logColumn("TIME", LogEntry::time);
        TableColumn<LogEntry,String> level = logColumn("LEVEL", LogEntry::level);
        TableColumn<LogEntry,String> component = logColumn("COMPONENT", LogEntry::component);
        TableColumn<LogEntry,String> message = logColumn("MESSAGE", LogEntry::message);
        time.setPrefWidth(115); level.setPrefWidth(105); component.setPrefWidth(155); message.setPrefWidth(620);
        level.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("level-info","level-traffic","level-warning","level-error","level-detection","level-defense");
                if (empty || item == null) { setText(null); return; }
                setText("[" + item + "]");
                getStyleClass().add("level-" + item.toLowerCase());
            }
        });
        table.getColumns().add(time); table.getColumns().add(level); table.getColumns().add(component); table.getColumns().add(message);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No log entries match the selected filter"));
        return table;
    }

    private TableColumn<LogEntry,String> logColumn(String title, java.util.function.Function<LogEntry,String> mapper) {
        TableColumn<LogEntry,String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(mapper.apply(cell.getValue())));
        return column;
    }

    private Button categoryButton(String title, String style, ComboBox<String> filter) {
        Button button = new Button(title);
        button.getStyleClass().addAll("log-category", "log-category-" + style);
        button.setOnAction(event -> filter.setValue(title));
        return button;
    }

    private void applyLogFilter(String level, String keyword) {
        String wantedLevel = level == null ? "ALL" : level;
        String wantedText = keyword == null ? "" : keyword.trim().toLowerCase();
        filteredLogs.setPredicate(entry ->
                ("ALL".equals(wantedLevel) || wantedLevel.equals(entry.level())) &&
                (wantedText.isEmpty() || entry.component().toLowerCase().contains(wantedText) ||
                        entry.message().toLowerCase().contains(wantedText)));
    }

    private long countLevel(String level) { return logEntries.stream().filter(entry -> level.equals(entry.level())).count(); }

    private void clearAllLogs() {
        logEntries.clear(); fullLog.clear(); dashboardLog.clear(); dosLog.clear(); ddosLog.clear(); connectionLog.clear();
    }

    private String componentFor(String message) {
        String text = message.toLowerCase();
        if (text.contains("connection") || text.contains("target")) return "TargetServer";
        if (text.contains("ddos") || text.contains("worker")) return "DDoS Simulator";
        if (text.contains("dos")) return "DoS Simulator";
        if (text.contains("rate") || text.contains("request")) return "Traffic";
        return "Application";
    }

    private VBox cyberPanel(String title, Node... content) {
        Label heading = sectionLabel(title);
        VBox panel = new VBox(10, heading);
        panel.getChildren().addAll(content);
        panel.getStyleClass().add("cyber-panel");
        panel.setPadding(new Insets(11));
        return panel;
    }

    private Label sectionLabel(String text) { Label label = new Label(text); label.getStyleClass().add("section-title"); return label; }
    private Label value(String text) { Label label = new Label(text); label.getStyleClass().add("value-text"); return label; }
    private Separator divider() { Separator separator = new Separator(javafx.geometry.Orientation.VERTICAL); separator.setPrefHeight(60); return separator; }
    private VBox pair(String title, Label value) { return new VBox(2, new Label(title), value); }
    private VBox statusColumn(String title, Node value) { VBox box = new VBox(5, new Label(title), value); box.getStyleClass().add("status-column"); return box; }
    private VBox buildServerInformation() { return new VBox(9, pair("Target IP", value(target.host.getText())), pair("Port", value(target.port.getText())), pair("Protocol", value("HTTP")), pair("Status", connectionLabel)); }
    private VBox pageHeader(String title, String subtitle) { Label h = new Label(title); h.getStyleClass().add("page-title"); Label s = new Label(subtitle); s.getStyleClass().add("page-subtitle"); return new VBox(3, h, s); }
    private ScrollPane page(Node node) { ScrollPane scroll = new ScrollPane(node); scroll.setFitToWidth(true); scroll.getStyleClass().add("dashboard-scroll"); return scroll; }
    private TextArea terminal() { TextArea area = new TextArea(); area.setEditable(false); area.setWrapText(false); area.getStyleClass().add("terminal-log"); return area; }
    private Button successButton(String text) { Button button = new Button(text); button.setGraphic(CyberIcon.of(text.contains("TEST") ? CyberIcon.Type.TARGET : CyberIcon.Type.PLAY, 15, "button-icon")); button.getStyleClass().addAll("cyber-button", "cyber-button-success"); return button; }
    private Button dangerButton(String text) { Button button = new Button(text); button.setGraphic(CyberIcon.of(CyberIcon.Type.STOP, 14, "button-icon")); button.getStyleClass().addAll("cyber-button", "cyber-button-danger"); return button; }
    private Button secondaryButton(String text) { Button button = new Button(text); CyberIcon.Type icon=text.contains("RESET")?CyberIcon.Type.RESET:text.contains("EXPORT")?CyberIcon.Type.EXPORT:text.contains("CLEAR")?CyberIcon.Type.TRASH:CyberIcon.Type.TARGET; button.setGraphic(CyberIcon.of(icon, 14, "button-icon")); button.getStyleClass().addAll("cyber-button", "cyber-button-secondary"); return button; }
    private Button smallButton(String text) { Button button = secondaryButton(text); button.getStyleClass().add("small-button"); return button; }
    private void addSummaryRow(GridPane grid, int row, String name, Label value) { grid.add(new Label(name), 0, row); grid.add(value, 1, row); }
    private void addInfoRow(GridPane grid, int row, String name, Label value) { Label key = new Label(name); key.getStyleClass().add("info-key"); grid.add(key, 0, row); grid.add(value, 1, row); }

    private void styleExistingButtons() {
        target.test.getStyleClass().addAll("cyber-button", "cyber-button-secondary");
        target.test.setGraphic(CyberIcon.of(CyberIcon.Type.TARGET, 14, "button-icon"));
        dos.start.getStyleClass().addAll("cyber-button", "cyber-button-success");
        dos.start.setGraphic(CyberIcon.of(CyberIcon.Type.PLAY, 14, "button-icon"));
        ddos.start.getStyleClass().addAll("cyber-button", "cyber-button-success");
        ddos.start.setGraphic(CyberIcon.of(CyberIcon.Type.PLAY, 14, "button-icon"));
        dos.stop.getStyleClass().addAll("cyber-button", "cyber-button-danger");
        dos.stop.setGraphic(CyberIcon.of(CyberIcon.Type.STOP, 14, "button-icon"));
        ddos.stop.getStyleClass().addAll("cyber-button", "cyber-button-danger");
        ddos.stop.setGraphic(CyberIcon.of(CyberIcon.Type.STOP, 14, "button-icon"));
    }

    private record LogEntry(String time, String level, String component, String message) { }
}
