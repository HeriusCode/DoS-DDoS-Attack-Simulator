package client.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Function;

/** System Logs tab, including filters and live counters. */
final class LogsTab {
    interface Actions {
        void clearAll();
        void exportLog();
        void applyFilter(String level, String keyword);
        long countLevel(String level);
    }

    private final ObservableList<LogEntry> entries;
    private final FilteredList<LogEntry> filtered;
    private final Actions actions;

    LogsTab(ObservableList<LogEntry> entries, FilteredList<LogEntry> filtered, Actions actions) {
        this.entries = entries;
        this.filtered = filtered;
        this.actions = actions;
    }

    Node build() {
        TableView<LogEntry> table = createTable();
        table.setItems(filtered);

        ComboBox<String> filter = new ComboBox<>();
        filter.getItems().addAll("ALL", "INFO", "TRAFFIC", "WARNING", "ERROR", "DETECTION", "DEFENSE");
        filter.setValue("ALL");
        ComboBox<String> timeRange = new ComboBox<>();
        timeRange.getItems().addAll("Current session", "Last 1 hour");
        timeRange.setValue("Last 1 hour");
        TextField search = new TextField();
        search.setPromptText("Enter keyword...");
        search.textProperty().addListener((observable, oldValue, newValue) ->
                actions.applyFilter(filter.getValue(), newValue));
        filter.valueProperty().addListener((observable, oldValue, newValue) ->
                actions.applyFilter(newValue, search.getText()));

        ToggleGroup categoryGroup = new ToggleGroup();
        ToggleButton all = category("ALL", "all", CyberIcon.Type.LIST, filter, categoryGroup);
        ToggleButton infoCategory = category("INFO", "info", CyberIcon.Type.INFO, filter, categoryGroup);
        ToggleButton trafficCategory = category("TRAFFIC", "traffic", CyberIcon.Type.TRAFFIC, filter, categoryGroup);
        ToggleButton warningCategory = category("WARNING", "warning", CyberIcon.Type.WARNING, filter, categoryGroup);
        ToggleButton errorCategory = category("ERROR", "error", CyberIcon.Type.ERROR, filter, categoryGroup);
        ToggleButton detectionCategory = category("DETECTION", "detection", CyberIcon.Type.TARGET, filter, categoryGroup);
        ToggleButton defenseCategory = category("DEFENSE", "defense", CyberIcon.Type.SHIELD, filter, categoryGroup);
        List<ToggleButton> categories = List.of(all, infoCategory, trafficCategory, warningCategory,
                errorCategory, detectionCategory, defenseCategory);
        categoryGroup.selectToggle(all);
        filter.valueProperty().addListener((observable, oldValue, value) -> categories.stream()
                .filter(button -> button.getText().equals(value))
                .findFirst().ifPresent(categoryGroup::selectToggle));

        HBox levels = new HBox(9, all, infoCategory, trafficCategory, warningCategory,
                errorCategory, detectionCategory, defenseCategory);
        levels.setAlignment(Pos.CENTER_LEFT);
        Button clear = ViewSupport.secondaryButton("CLEAR");
        Button export = ViewSupport.secondaryButton("EXPORT");
        clear.setOnAction(event -> actions.clearAll());
        export.setOnAction(event -> actions.exportLog());
        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        HBox tools = new HBox(8, levels, toolbarSpacer, clear, export);
        tools.setAlignment(Pos.CENTER_LEFT);
        tools.getStyleClass().add("logs-toolbar");

        VBox filters = ViewSupport.panel("LOG FILTER", CyberIcon.Type.FILTER,
                filterField("LOG TYPE", CyberIcon.Type.LIST, filter),
                filterField("TIME RANGE", CyberIcon.Type.CALENDAR, timeRange),
                filterField("SEARCH", CyberIcon.Type.SEARCH, search));
        filters.getStyleClass().addAll("logs-side-panel", "logs-filter-panel");
        filters.setMinHeight(0);
filters.setPrefHeight(250);
filters.setMaxHeight(250);

        Label total = statValue("0", "stat-total");
        Label info = statValue("0", "stat-info");
        Label traffic = statValue("0", "stat-traffic");
        Label warning = statValue("0", "stat-warning");
        Label error = statValue("0", "stat-error");
        Label detection = statValue("0", "stat-detection");
        Label defense = statValue("0", "stat-defense");
        GridPane counters = new GridPane();
        counters.setVgap(8);
        ColumnConstraints counterName = new ColumnConstraints();
        counterName.setPercentWidth(68);
        ColumnConstraints counterValue = new ColumnConstraints();
        counterValue.setPercentWidth(32);
        counterValue.setHalignment(HPos.RIGHT);
        counters.getColumnConstraints().addAll(counterName, counterValue);
        addCounter(counters, 0, "Total Logs", total);
        addCounter(counters, 1, "Info", info);
        addCounter(counters, 2, "Traffic", traffic);
        addCounter(counters, 3, "Warning", warning);
        addCounter(counters, 4, "Error", error);
        addCounter(counters, 5, "Detection", detection);
        addCounter(counters, 6, "Defense", defense);
        VBox quickStats = ViewSupport.panel("QUICK STATS", CyberIcon.Type.CHART, counters);
quickStats.getStyleClass().addAll("logs-side-panel", "logs-stats-panel");

// Không cho QUICK STATS kéo dài theo chiều cao của cột
quickStats.setMinHeight(0);
quickStats.setPrefHeight(205);
quickStats.setMaxHeight(205);

        ToggleButton autoScroll = liveSwitch();
        Label liveSubtitle = new Label("Auto scroll enabled");
        liveSubtitle.getStyleClass().add("live-logs-subtitle");
        autoScroll.selectedProperty().addListener((observable, oldValue, enabled) -> {
            autoScroll.setAlignment(enabled ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            liveSubtitle.setText(enabled ? "Auto scroll enabled" : "Auto scroll disabled");
        });
        Label liveTitle = new Label("LIVE LOGS");
        liveTitle.getStyleClass().add("live-logs-title");
        VBox liveCopy = new VBox(2, liveTitle, liveSubtitle);
        liveCopy.getStyleClass().add("live-logs-copy");
        Region liveSpacer = new Region();
        HBox.setHgrow(liveSpacer, Priority.ALWAYS);
        HBox liveContent = new HBox(10, CyberIcon.of(CyberIcon.Type.PULSE, 30, "live-logs-icon"),
                liveCopy, liveSpacer, autoScroll);
        liveContent.setAlignment(Pos.CENTER_LEFT);
        VBox live = new VBox(liveContent);
        live.getStyleClass().addAll("cyber-panel", "logs-side-panel", "live-logs-panel");
        live.setPadding(new Insets(10, 12, 10, 12));
        live.setMinHeight(0);
live.setPrefHeight(60);
live.setMaxHeight(60);

        entries.addListener((javafx.collections.ListChangeListener<LogEntry>) change -> {
            if (autoScroll.isSelected() && !filtered.isEmpty()) {
                Platform.runLater(() -> table.scrollTo(filtered.size() - 1));
            }
        });
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            total.setText(Integer.toString(entries.size()));
            info.setText(Long.toString(actions.countLevel("INFO")));
            traffic.setText(Long.toString(actions.countLevel("TRAFFIC")));
            warning.setText(Long.toString(actions.countLevel("WARNING")));
            error.setText(Long.toString(actions.countLevel("ERROR")));
            detection.setText(Long.toString(actions.countLevel("DETECTION")));
            defense.setText(Long.toString(actions.countLevel("DEFENSE")));
        }));
        update.setCycleCount(Animation.INDEFINITE);
        update.play();

        VBox tablePanel = new VBox(table);
        tablePanel.getStyleClass().addAll("cyber-panel", "logs-table-panel");
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMaxHeight(Double.MAX_VALUE);

        VBox right = new VBox(10, filters, quickStats, live);
        right.setPrefWidth(290);
        right.setMinWidth(270);
        right.setMaxHeight(Double.MAX_VALUE);
        right.getStyleClass().add("logs-right-column");

        HBox body = new HBox(12, tablePanel, right);
        body.setFillHeight(true);
        body.setMinHeight(0);
        body.setPrefHeight(530);
        body.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);

        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        tablePanel.setMinHeight(0);
        tablePanel.setMaxHeight(Double.MAX_VALUE);

        VBox root = new VBox(12,
                ViewSupport.pageHeader(CyberIcon.Type.LOG, "SYSTEM LOGS",
                        "View and monitor all system events, attacks and simulation logs"),
                tools,
                body);
        root.getStyleClass().add("logs-view");
        return ViewSupport.page(root);
    }

    private VBox filterField(String title, CyberIcon.Type icon, Control control) {
        Label caption = new Label(title);
        caption.getStyleClass().add("logs-filter-caption");
        control.setMaxWidth(Double.MAX_VALUE);
        control.getStyleClass().add("logs-filter-control");
        HBox shell = new HBox(9, CyberIcon.of(icon, 16, "logs-filter-field-icon"), control);
        shell.setAlignment(Pos.CENTER_LEFT);
        shell.getStyleClass().add("logs-filter-field");
        HBox.setHgrow(control, Priority.ALWAYS);
        return new VBox(6, caption, shell);
    }

    private ToggleButton liveSwitch() {
        Circle knob = new Circle(7);
        knob.getStyleClass().add("live-switch-knob");
        ToggleButton toggle = new ToggleButton("", knob);
        toggle.getStyleClass().add("live-log-switch");
        toggle.setFocusTraversable(false);
        toggle.setMinSize(46, 24);
        toggle.setPrefSize(46, 24);
        toggle.setMaxSize(46, 24);
        toggle.setSelected(true);
        toggle.setAlignment(Pos.CENTER_RIGHT);
        return toggle;
    }

    private Label statValue(String text, String styleClass) {
        Label label = ViewSupport.value(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private void addCounter(GridPane grid, int row, String title, Label value) {
        Label key = new Label(title);
        key.getStyleClass().add("logs-stat-key");
        grid.add(key, 0, row);
        grid.add(value, 1, row);
        GridPane.setHalignment(value, HPos.RIGHT);
    }

    private TableView<LogEntry> createTable() {
        TableView<LogEntry> table = new TableView<>();
        table.getStyleClass().add("system-log-table");
        TableColumn<LogEntry, String> time = column("TIME", LogEntry::time);
        TableColumn<LogEntry, String> level = column("LEVEL", LogEntry::level);
        TableColumn<LogEntry, String> component = column("COMPONENT", LogEntry::component);
        TableColumn<LogEntry, String> message = column("MESSAGE", LogEntry::message);
        time.setPrefWidth(115);
        level.setPrefWidth(105);
        component.setPrefWidth(155);
        message.setPrefWidth(620);
        level.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("level-info", "level-traffic", "level-warning",
                        "level-error", "level-detection", "level-defense");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText("[" + item + "]");
                getStyleClass().add("level-" + item.toLowerCase());
            }
        });
        table.getColumns().add(time);
        table.getColumns().add(level);
        table.getColumns().add(component);
        table.getColumns().add(message);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No log entries match the selected filter"));
        return table;
    }

    private TableColumn<LogEntry, String> column(String title, Function<LogEntry, String> mapper) {
        TableColumn<LogEntry, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(mapper.apply(cell.getValue())));
        return column;
    }

    private ToggleButton category(String title, String style, CyberIcon.Type icon,
                                  ComboBox<String> filter, ToggleGroup group) {
        ToggleButton button = new ToggleButton(title, CyberIcon.of(icon, 15, "log-category-icon"));
        button.setToggleGroup(group);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll("log-category", "log-category-" + style);
        button.setOnAction(event -> {
            if (!button.isSelected()) group.selectToggle(button);
            filter.setValue(title);
        });
        return button;
    }
}
