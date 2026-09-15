package client.view;

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

/** Dashboard tab layout. Business actions are supplied by MainDashboard. */
final class DashboardTab {
    interface Actions {
        void testConnection();
        void reset();
        void exportLog();
        void clearDashboardLog();
    }

    private final StatisticsPanel statistics;
    private final LineChart<Number, Number> rateChart;
    private final TextArea dashboardLog;
    private final TargetPanel target;
    private final Label connection;
    private final Button start;
    private final Button stop;
    private final Actions actions;

    DashboardTab(StatisticsPanel statistics, LineChart<Number, Number> rateChart, TextArea dashboardLog,
                 TargetPanel target, Label connection, Button start, Button stop, Actions actions) {
        this.statistics = statistics;
        this.rateChart = rateChart;
        this.dashboardLog = dashboardLog;
        this.target = target;
        this.connection = connection;
        this.start = start;
        this.stop = stop;
        this.actions = actions;
    }

    Node build() {
        VBox content = new VBox(12);
        VBox statsAndChart = ViewSupport.panel("REAL-TIME STATISTICS", statistics,
                ViewSupport.sectionTitle("Request Rate  (requests/sec)"), rateChart);
        VBox logPanel = logPanel();
        HBox middle = new HBox(12, statsAndChart, logPanel);
        HBox.setHgrow(statsAndChart, Priority.ALWAYS);
        HBox.setHgrow(logPanel, Priority.ALWAYS);
        statsAndChart.setPrefWidth(690);
        logPanel.setPrefWidth(500);

        VBox targetCard = targetCard();
        VBox quickActions = quickActions();
        VBox dataStream = dataStreamCard();
        HBox bottom = new HBox(12, targetCard, quickActions, dataStream);
        HBox.setHgrow(targetCard, Priority.ALWAYS);
        HBox.setHgrow(quickActions, Priority.ALWAYS);
        HBox.setHgrow(dataStream, Priority.ALWAYS);
        targetCard.setPrefWidth(390);
        quickActions.setPrefWidth(310);
        dataStream.setPrefWidth(360);

        content.getChildren().addAll(middle, bottom);
        return ViewSupport.page(content);
    }

    private VBox logPanel() {
        Button clear = ViewSupport.smallButton("CLEAR");
        Button export = ViewSupport.smallButton("EXPORT");
        clear.setOnAction(event -> actions.clearDashboardLog());
        export.setOnAction(event -> actions.exportLog());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, ViewSupport.sectionTitle("LOG"), spacer, clear, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(9, toolbar, dashboardLog);
        panel.getStyleClass().add("cyber-panel");
        panel.setPadding(new Insets(10));
        VBox.setVgrow(dashboardLog, Priority.ALWAYS);
        dashboardLog.setPrefHeight(360);
        return panel;
    }

    private VBox targetCard() {
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

        Button test = ViewSupport.successButton("TEST CONNECTION");
        test.setMaxWidth(Double.MAX_VALUE);
        test.setOnAction(event -> actions.testConnection());
        Label state = new Label();
        state.getStyleClass().add("status-connected");
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> state.setText(connection.getText())));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();
        return ViewSupport.panel("TARGET SERVER CONFIGURATION", form, test, state);
    }

    private VBox quickActions() {
        start.setMaxWidth(Double.MAX_VALUE);
        stop.setMaxWidth(Double.MAX_VALUE);
        Button reset = ViewSupport.secondaryButton("RESET");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(event -> actions.reset());
        return ViewSupport.panel("QUICK ACTIONS", start, stop, reset);
    }

    private VBox dataStreamCard() {
        DataStreamView stream = new DataStreamView();
        stream.setMinHeight(218);
        stream.setPrefHeight(218);
        VBox box = new VBox(stream);
        box.getStyleClass().add("data-stream-card");
        VBox.setVgrow(stream, Priority.ALWAYS);
        return box;
    }
}
