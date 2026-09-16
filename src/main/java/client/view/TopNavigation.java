package client.view;

import javafx.geometry.Pos;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TopNavigation extends VBox {
    /** A hairline gap keeps the cyber-tab outlines readable without breaking the baseline. */
    private static final double TAB_GAP = 1;
    private static final PseudoClass ATTACK_ACTIVE = PseudoClass.getPseudoClass("attack-active");

    private final HBox tabStrip = new HBox(TAB_GAP);
    private final StackPane contentHost = new StackPane();
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final List<ToggleButton> buttons = new ArrayList<>();
    private List<Node> views = List.of();
    private Consumer<Boolean> attackModeChanged = mode -> { };

    public TopNavigation() {
        getStyleClass().add("top-navigation");

        tabStrip.getStyleClass().add("top-nav-tabs");
        tabStrip.setAlignment(Pos.CENTER_LEFT);

        StackPane header = new StackPane(tabStrip);
        header.getStyleClass().add("top-nav-header");
        StackPane.setAlignment(tabStrip, Pos.CENTER_LEFT);

        contentHost.getStyleClass().add("top-nav-content");
        getChildren().addAll(header, contentHost);
        VBox.setVgrow(contentHost, Priority.ALWAYS);
    }

    public void configure(
            Node dashboardView,
            Node targetView,
            Node dosView,
            Node ddosView,
            Node logsView,
            Node settingsView,
            Consumer<Boolean> attackModeChanged
    ) {
        this.attackModeChanged = attackModeChanged == null ? mode -> { } : attackModeChanged;
        views = List.of(dashboardView, targetView, dosView, ddosView, logsView, settingsView);

        buttons.clear();
        tabStrip.getChildren().clear();
        toggleGroup.getToggles().clear();

        addTab("Dashboard", CyberIcon.Type.HOME, "dashboard-tab", true);
        addTab("Target Server", CyberIcon.Type.SERVER, "target-tab", false);
        addTab("DoS Attack", CyberIcon.Type.LIGHTNING, "dos-tab", false);
        addTab("DDoS Attack", CyberIcon.Type.NETWORK, "ddos-tab", false);
        addTab("Logs", CyberIcon.Type.LOG, "logs-tab", false);
        addTab("Settings", CyberIcon.Type.SETTINGS, "settings-tab", false);

        selectTab(0);
    }

    private void addTab(String title, CyberIcon.Type icon, String styleClass, boolean first) {
        int index = buttons.size();
        ToggleButton button = new ToggleButton(title, CyberIcon.of(icon, 14, "tab-icon"));
        button.setToggleGroup(toggleGroup);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll("top-nav-tab", styleClass);
        if (first) {
            button.getStyleClass().add("first-tab");
        }
        button.setOnAction(event -> selectTab(index));

        buttons.add(button);
        tabStrip.getChildren().add(button);
    }

    public void selectTab(int index) {
        if (index < 0 || index >= buttons.size()) {
            return;
        }

        ToggleButton selected = buttons.get(index);
        toggleGroup.selectToggle(selected);
        buttons.forEach(button -> button.setViewOrder(button == selected ? -1 : 0));
        pseudoClassStateChanged(ATTACK_ACTIVE, index == 2 || index == 3);
        contentHost.getChildren().setAll(views.get(index));

        if (index == 2) {
            attackModeChanged.accept(false);
        } else if (index == 3) {
            attackModeChanged.accept(true);
        }
    }
}
