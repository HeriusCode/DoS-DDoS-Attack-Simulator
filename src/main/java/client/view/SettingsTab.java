package client.view;

import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

/** Settings screen. The controls intentionally remain local UI preferences. */
final class SettingsTab {
    interface Actions {
        void clearLogs();
        void selectPreset(String host, String port, String address);
    }

    private final Actions actions;

    SettingsTab(Actions actions) {
        this.actions = actions;
    }

    Node build() {
        HBox header = settingsHeader();

        VBox left = new VBox(12, applicationSettings(), simulationSafety(), networkSettings());
        VBox center = new VBox(12, logDataManagement(), uiDisplay(), about());
        VBox right = new VBox(12, quickPresets(), safetyReminder(), learningCard());
        left.getStyleClass().add("settings-column");
        center.getStyleClass().add("settings-column");
        right.getStyleClass().addAll("settings-column", "settings-side-column");

        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(center, Priority.ALWAYS);
        left.setPrefWidth(500);
        center.setPrefWidth(455);
        right.setPrefWidth(265);
        right.setMinWidth(235);

        HBox body = new HBox(14, left, center, right);
        body.setAlignment(Pos.TOP_LEFT);
        body.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(12, header, body);
        root.getStyleClass().add("settings-view");
        return ViewSupport.page(root);
    }

    private HBox settingsHeader() {
        HBox header = ViewSupport.pageHeader(CyberIcon.Type.SETTINGS, "SETTINGS",
                "Configure application, simulation and system preferences");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label mode = new Label("SECURE   |   MONITOR   |   SIMULATE");
        mode.getStyleClass().add("settings-mode-label");
        header.getChildren().addAll(spacer, mode);
        return header;
    }

    private VBox applicationSettings() {
        ComboBox<String> theme = combo("Dark Cybersecurity", "Midnight Blue");
        ComboBox<String> level = combo("INFO", "TRAFFIC", "WARNING", "ERROR");
        TextField interval = field("1000");
        return panel("APPLICATION SETTINGS", CyberIcon.Type.MONITOR,
                row(CyberIcon.Type.SETTINGS, "Theme", theme, null),
                row(CyberIcon.Type.LIST, "Log Level", level, null),
                row(CyberIcon.Type.CHART, "Chart Update Interval", interval, "ms"),
                toggleRow(CyberIcon.Type.LOG, "Auto Save Logs", true, "Automatically save logs to file"));
    }

    private VBox simulationSafety() {
        Label warning = new Label("These limits are for safety purposes only.\nYou can adjust them for your private lab environment.",
                CyberIcon.of(CyberIcon.Type.WARNING, 19, "settings-warning-icon"));
        warning.getStyleClass().add("settings-warning");
        warning.setMaxWidth(Double.MAX_VALUE);
        return panel("SIMULATION SAFETY", CyberIcon.Type.SHIELD,
                row(CyberIcon.Type.NETWORK, "Maximum Requests/sec", field("100"), "req/sec     (1 - 1000)"),
                row(CyberIcon.Type.CLOCK, "Maximum Duration", field("60"), "seconds     (10 - 3600)"),
                row(CyberIcon.Type.NETWORK, "Maximum DDoS Nodes", field("20"), "nodes       (1 - 100)"),
                warning);
    }

    private VBox networkSettings() {
        ComboBox<String> protocol = combo("HTTP", "HTTPS");
        return panel("NETWORK SETTINGS", CyberIcon.Type.WIFI,
                row(CyberIcon.Type.NETWORK, "Default Protocol", protocol, null),
                row(CyberIcon.Type.LINK, "Connection Timeout", field("5000"), "ms"),
                row(CyberIcon.Type.LINK, "Request Timeout", field("10000"), "ms"),
                row(CyberIcon.Type.RESET, "Retry Count", field("3"), "times"));
    }

    private VBox logDataManagement() {
        TextField path = field("./logs/simulator.log");
        Button browse = new Button("", CyberIcon.of(CyberIcon.Type.FOLDER, 15, "button-icon"));
        browse.getStyleClass().addAll("cyber-button", "cyber-button-secondary", "settings-browse");
        browse.setOnAction(event -> chooseLogPath(path));
        HBox pathControl = new HBox(7, path, browse);
        HBox.setHgrow(path, Priority.ALWAYS);
        pathControl.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> size = combo("10", "25", "50", "100");
        Button clear = ViewSupport.secondaryButton("CLEAR LOGS");
        clear.getStyleClass().add("settings-clear-logs");
        clear.setOnAction(event -> actions.clearLogs());
        HBox clearRow = new HBox(clear);
        clearRow.setAlignment(Pos.CENTER_RIGHT);

        return panel("LOG & DATA MANAGEMENT", CyberIcon.Type.FOLDER,
                row(CyberIcon.Type.SETTINGS, "Log File Path", pathControl, null),
                row(CyberIcon.Type.LIST, "Max Log File Size", size, "MB"),
                toggleRow(CyberIcon.Type.LOG, "Log Rotation", true, "Keep multiple log files"),
                row(CyberIcon.Type.CLIPBOARD, "Max Log Files", field("5"), "files"),
                row(CyberIcon.Type.CALENDAR, "Data Retention", field("7"), "days"),
                clearRow);
    }

    private VBox uiDisplay() {
        ComboBox<String> language = combo("English", "Tiếng Việt");
        return panel("UI & DISPLAY", CyberIcon.Type.MONITOR,
                row(CyberIcon.Type.NETWORK, "Language", language, null),
                toggleRow(CyberIcon.Type.SETTINGS, "Show Tooltips", true, "Display helpful tooltips"),
                toggleRow(CyberIcon.Type.SETTINGS, "Animations", true, "Enable UI animations"),
                toggleRow(CyberIcon.Type.DASHBOARD, "Compact Mode", false, "Reduce panel padding"));
    }

    private VBox about() {
        VBox values = new VBox(5,
                aboutLine("Application Name", "DOS/DDoS Attack Simulator"),
                aboutLine("Version", "1.0.0"),
                aboutLine("Build Date", "2026-09-17"),
                aboutLine("Developer", "Educational Project"),
                aboutLine("License", "For Educational Use Only"));
        return panel("ABOUT", CyberIcon.Type.INFO, values);
    }

    private VBox quickPresets() {
        Label caption = new Label("Target Server");
        caption.getStyleClass().add("settings-preset-caption");
        return panel("QUICK PRESETS", CyberIcon.Type.LIGHTNING, caption,
                preset("Local Test", "127.0.0.1:8080", "127.0.0.1", "8080"),
                preset("Lab Server", "192.168.1.20:8080", "192.168.1.20", "8080"),
                preset("Test Server", "10.0.0.5:8080", "10.0.0.5", "8080"));
    }

    private VBox safetyReminder() {
        VBox bullets = new VBox(8,
                bullet("Use only for educational purposes."),
                bullet("Do not test on public networks."),
                bullet("Ensure you have permission before testing any server."),
                bullet("This tool is for learning and research only."));
        return panel("SAFETY REMINDER", CyberIcon.Type.SHIELD, bullets);
    }

    private VBox learningCard() {
        Label message = new Label("LEARN TODAY\nBUILD A SAFER\nTOMORROW",
                CyberIcon.of(CyberIcon.Type.LOCK, 42, "settings-learning-icon"));
        message.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
        message.setAlignment(Pos.CENTER);
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        message.getStyleClass().add("settings-learning-copy");
        VBox card = new VBox(message);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("settings-learning-card");
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox panel(String title, CyberIcon.Type icon, Node... content) {
        Label heading = ViewSupport.sectionTitle(title, icon);
        heading.setMaxWidth(Double.MAX_VALUE);
        heading.getStyleClass().add("settings-panel-title");
        VBox panel = new VBox(9, heading);
        panel.getChildren().addAll(content);
        panel.setPadding(new Insets(10, 12, 11, 12));
        panel.getStyleClass().addAll("cyber-panel", "settings-panel");
        return panel;
    }

    private HBox row(CyberIcon.Type icon, String name, Node control, String suffix) {
        Label label = new Label(name);
        label.getStyleClass().add("settings-row-label");
        label.setMinWidth(142);
        HBox.setHgrow(control, Priority.ALWAYS);
        if (control instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(9, CyberIcon.of(icon, 17, "settings-row-icon"), label, control);
        if (suffix != null) {
            Label suffixLabel = new Label(suffix);
            suffixLabel.getStyleClass().add("settings-row-suffix");
            row.getChildren().add(suffixLabel);
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-row");
        return row;
    }

    private HBox toggleRow(CyberIcon.Type icon, String name, boolean enabled, String hint) {
        ToggleButton toggle = settingToggle(enabled);
        Label description = new Label(hint);
        description.setWrapText(true);
        description.getStyleClass().add("settings-toggle-hint");
        HBox control = new HBox(10, toggle, description);
        control.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(description, Priority.ALWAYS);
        return row(icon, name, control, null);
    }

    private ToggleButton settingToggle(boolean selected) {
        Circle knob = new Circle(6, Color.web("#9ffff0"));
        ToggleButton toggle = new ToggleButton("", knob);
        toggle.getStyleClass().add("settings-toggle");
        toggle.setSelected(selected);
        return toggle;
    }

    private Button preset(String name, String address, String host, String port) {
        Label title = new Label(name);
        Label value = new Label(address);
        title.getStyleClass().add("settings-preset-name");
        value.getStyleClass().add("settings-preset-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox graphic = new HBox(8, title, spacer, value);
        graphic.setAlignment(Pos.CENTER_LEFT);
        Button button = new Button();
        button.setGraphic(graphic);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("settings-preset-button");
        button.setOnAction(event -> actions.selectPreset(host, port, address));
        return button;
    }

    private HBox aboutLine(String key, String value) {
        Label keyLabel = new Label(key + "  :");
        keyLabel.getStyleClass().add("settings-about-key");
        keyLabel.setMinWidth(125);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("settings-about-value");
        return new HBox(8, keyLabel, valueLabel);
    }

    private Label bullet(String text) {
        Label label = new Label("•  " + text);
        label.setWrapText(true);
        label.getStyleClass().add("settings-reminder-line");
        return label;
    }

    private ComboBox<String> combo(String selected, String... alternatives) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add(selected);
        combo.getItems().addAll(alternatives);
        combo.setValue(selected);
        combo.getStyleClass().add("settings-control");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private TextField field(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("settings-control");
        return field;
    }

    private void chooseLogPath(TextField path) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select log file");
        chooser.setInitialFileName("simulator.log");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log files", "*.log", "*.txt"));
        File file = chooser.showSaveDialog(path.getScene().getWindow());
        if (file != null) path.setText(file.getPath());
    }
}
