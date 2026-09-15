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
        TargetServerTab targetTab = new TargetServerTab(target, connectionLabel, responseTimeLabel,
                httpStatusLabel, lastCheckedLabel, connectionLog, new TargetServerTab.Actions() {
            @Override public void testConnection() { MainDashboard.this.testConnection(); }
            @Override public void selectPreset(String host, String port, String address) {
                MainDashboard.this.selectTargetPreset(host, port, address);
            }
        });
        DashboardTab dashboardTab = new DashboardTab(statistics, rateChart, dashboardLog, target,
                connectionLabel, dashboardStart, dashboardStop, new DashboardTab.Actions() {
            @Override public void testConnection() { MainDashboard.this.testConnection(); }
            @Override public void reset() { MainDashboard.this.resetVisuals(); }
            @Override public void exportLog() { MainDashboard.this.exportLog(); }
            @Override public void clearDashboardLog() { MainDashboard.this.clearDashboardLogs(); }
        });
        DosSimulationTab dosTab = new DosSimulationTab(target, dos, dosStatistics, dosRateChart, dosLog,
                connectionLabel, controller::snapshot, typeLabel::getText, controller::state,
                new DosSimulationTab.Actions() {
                    @Override public void testConnection() { MainDashboard.this.testConnection(); }
                    @Override public void exportLog() { MainDashboard.this.exportLog(); }
                });
        DdosSimulationTab ddosTab = new DdosSimulationTab(target, ddos, ddosStatistics, ddosRateChart, ddosLog,
                nodeTable, connectionLabel, controller::snapshot, typeLabel::getText, controller::state,
                () -> !verifiedTarget.isBlank(), new DdosSimulationTab.Actions() {
                    @Override public void testConnection() { MainDashboard.this.testConnection(); }
                    @Override public void exportLog() { MainDashboard.this.exportLog(); }
                    @Override public void start() { MainDashboard.this.startSimulation(true); }
                    @Override public void stop() { MainDashboard.this.stopSimulation(); }
                    @Override public void reset() { MainDashboard.this.resetVisuals(); }
                });
        LogsTab logsTab = new LogsTab(logEntries, filteredLogs, new LogsTab.Actions() {
            @Override public void clearAll() { MainDashboard.this.clearAllLogs(); }
            @Override public void exportLog() { MainDashboard.this.exportLog(); }
            @Override public void applyFilter(String level, String keyword) { MainDashboard.this.applyLogFilter(level, keyword); }
            @Override public long countLevel(String level) { return MainDashboard.this.countLevel(level); }
        });
        tabs.configure(dashboardTab.build(), targetTab.build(), dosTab.build(), ddosTab.build(), logsTab.build(),
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

    private void selectTargetPreset(String host, String port, String address) {
        target.host.setText(host);
        target.port.setText(port);
        verifiedTarget = "";
        connectionLabel.setText("Not tested");
        target.status.setText("NOT TESTED");
        appendConnectionLog("INFO", "Preset selected: " + address + ". Connection test required.");
        updateControls();
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
        chart.setMinHeight(140);
        chart.setPrefHeight(170);
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

    private void clearDashboardLogs() {
        dashboardLog.clear();
        fullLog.clear();
        appendLog("INFO", "Log cleared");
    }

    private String componentFor(String message) {
        String text = message.toLowerCase();
        if (text.contains("connection") || text.contains("target")) return "TargetServer";
        if (text.contains("ddos") || text.contains("worker")) return "DDoS Simulator";
        if (text.contains("dos")) return "DoS Simulator";
        if (text.contains("rate") || text.contains("request")) return "Traffic";
        return "Application";
    }

    private Label value(String text) { Label label = new Label(text); label.getStyleClass().add("value-text"); return label; }
    private TextArea terminal() { TextArea area = new TextArea(); area.setEditable(false); area.setWrapText(false); area.getStyleClass().add("terminal-log"); return area; }
    private Button successButton(String text) { Button button = new Button(text); button.setGraphic(CyberIcon.of(text.contains("TEST") ? CyberIcon.Type.TARGET : CyberIcon.Type.PLAY, 15, "button-icon")); button.getStyleClass().addAll("cyber-button", "cyber-button-success"); return button; }
    private Button dangerButton(String text) { Button button = new Button(text); button.setGraphic(CyberIcon.of(CyberIcon.Type.STOP, 14, "button-icon")); button.getStyleClass().addAll("cyber-button", "cyber-button-danger"); return button; }

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
}
