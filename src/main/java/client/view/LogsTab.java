package client.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
        filter.getItems().addAll("ALL", "INFO", "TRAFFIC", "WARNING", "ERROR", "DETECTION", "DEFENSE"); filter.setValue("ALL");
        ComboBox<String> timeRange = new ComboBox<>(); timeRange.getItems().addAll("Current session", "Last 1 hour"); timeRange.setValue("Current session");
        TextField search = new TextField(); search.setPromptText("Enter keyword...");
        search.textProperty().addListener((observable, oldValue, newValue) -> actions.applyFilter(filter.getValue(), newValue));
        filter.valueProperty().addListener((observable, oldValue, newValue) -> actions.applyFilter(newValue, search.getText()));

        Button clear = ViewSupport.secondaryButton("CLEAR"), export = ViewSupport.secondaryButton("EXPORT");
        clear.setOnAction(event -> actions.clearAll()); export.setOnAction(event -> actions.exportLog());
        HBox levels = new HBox(9, category("ALL", "all", filter), category("INFO", "info", filter), category("TRAFFIC", "traffic", filter),
                category("WARNING", "warning", filter), category("ERROR", "error", filter), category("DETECTION", "detection", filter), category("DEFENSE", "defense", filter));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(8, levels, spacer, clear, export); tools.setAlignment(Pos.CENTER_LEFT);

        VBox filters = ViewSupport.panel("LOG FILTER", new Label("LOG TYPE"), filter, new Label("TIME RANGE"), timeRange, new Label("SEARCH"), search);
        filter.setMaxWidth(Double.MAX_VALUE); timeRange.setMaxWidth(Double.MAX_VALUE); search.setMaxWidth(Double.MAX_VALUE);
        Label total = ViewSupport.value("0"), info = ViewSupport.value("0"), traffic = ViewSupport.value("0"), warning = ViewSupport.value("0");
        Label error = ViewSupport.value("0"), detection = ViewSupport.value("0"), defense = ViewSupport.value("0");
        GridPane counters = new GridPane(); counters.setHgap(18); counters.setVgap(8);
        ViewSupport.addInfoRow(counters, 0, "Total Logs", total); ViewSupport.addInfoRow(counters, 1, "Info", info); ViewSupport.addInfoRow(counters, 2, "Traffic", traffic);
        ViewSupport.addInfoRow(counters, 3, "Warning", warning); ViewSupport.addInfoRow(counters, 4, "Error", error); ViewSupport.addInfoRow(counters, 5, "Detection", detection); ViewSupport.addInfoRow(counters, 6, "Defense", defense);
        VBox quickStats = ViewSupport.panel("QUICK STATS", counters);
        CheckBox autoScroll = new CheckBox("Auto scroll enabled"); autoScroll.setSelected(true);
        VBox live = ViewSupport.panel("LIVE LOGS", autoScroll);
        entries.addListener((javafx.collections.ListChangeListener<LogEntry>) change -> {
            if (autoScroll.isSelected() && !filtered.isEmpty()) Platform.runLater(() -> table.scrollTo(filtered.size() - 1));
        });
        Timeline update = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            total.setText(Integer.toString(entries.size())); info.setText(Long.toString(actions.countLevel("INFO"))); traffic.setText(Long.toString(actions.countLevel("TRAFFIC")));
            warning.setText(Long.toString(actions.countLevel("WARNING"))); error.setText(Long.toString(actions.countLevel("ERROR")));
            detection.setText(Long.toString(actions.countLevel("DETECTION"))); defense.setText(Long.toString(actions.countLevel("DEFENSE")));
        }));
        update.setCycleCount(Animation.INDEFINITE); update.play();
        VBox tablePanel = ViewSupport.panel("EVENT STREAM", table); VBox.setVgrow(table, Priority.ALWAYS); table.setPrefHeight(510);
        VBox right = new VBox(10, filters, quickStats, live); right.setPrefWidth(280);
        HBox body = new HBox(12, tablePanel, right); HBox.setHgrow(tablePanel, Priority.ALWAYS);
        VBox root = new VBox(12, ViewSupport.pageHeader("SYSTEM LOGS", "View and monitor all system events, attacks and simulation logs"), tools, body);
        return ViewSupport.page(root);
    }

    private TableView<LogEntry> createTable() {
        TableView<LogEntry> table = new TableView<>(); table.getStyleClass().add("system-log-table");
        TableColumn<LogEntry, String> time = column("TIME", LogEntry::time), level = column("LEVEL", LogEntry::level);
        TableColumn<LogEntry, String> component = column("COMPONENT", LogEntry::component), message = column("MESSAGE", LogEntry::message);
        time.setPrefWidth(115); level.setPrefWidth(105); component.setPrefWidth(155); message.setPrefWidth(620);
        level.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("level-info", "level-traffic", "level-warning", "level-error", "level-detection", "level-defense");
                if (empty || item == null) { setText(null); return; }
                setText("[" + item + "]"); getStyleClass().add("level-" + item.toLowerCase());
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

    private Button category(String title, String style, ComboBox<String> filter) {
        Button button = new Button(title); button.getStyleClass().addAll("log-category", "log-category-" + style);
        button.setOnAction(event -> filter.setValue(title)); return button;
    }
}
