package client.view;

import client.model.TrafficStatistics;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public final class DdosStatisticsPanel extends GridPane {
    private final MetricCard nodes = new MetricCard(CyberIcon.Type.NETWORK, "Active Nodes", "metric-cyan");
    private final MetricCard sent = new MetricCard(CyberIcon.Type.EXPORT, "Total Requests Sent", "metric-blue");
    private final MetricCard success = new MetricCard(CyberIcon.Type.SHIELD, "Successful", "metric-green");
    private final MetricCard failed = new MetricCard(CyberIcon.Type.LIGHTNING, "Failed / Limited", "metric-red");
    private final MetricCard rate = new MetricCard(CyberIcon.Type.CHART, "Current RPS", "metric-purple");
    private final MetricCard response = new MetricCard(CyberIcon.Type.TARGET, "Average Response", "metric-yellow");
    private final MetricCard elapsed = new MetricCard(CyberIcon.Type.RESET, "Elapsed Time", "metric-cyan");
    private final MetricCard remaining = new MetricCard(CyberIcon.Type.LOG, "Remaining Time", "metric-blue");

    public DdosStatisticsPanel() {
        setHgap(9); setVgap(9);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25); column.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(column);
        }
        add(nodes,0,0); add(sent,1,0); add(success,2,0); add(failed,3,0);
        add(rate,0,1); add(response,1,1); add(elapsed,2,1); add(remaining,3,1);
    }

    public void show(TrafficStatistics.Snapshot snapshot, int configuredNodes) {
        if (snapshot == null) {
            nodes.setValue("0 / " + configuredNodes); sent.setValue("0"); success.setValue("0"); failed.setValue("0");
            rate.setValue("0"); response.setValue("0 ms"); elapsed.setValue("00:00"); remaining.setValue("--:--"); return;
        }
        nodes.setValue(snapshot.activeWorkers() + " / " + configuredNodes);
        sent.setValue(Long.toString(snapshot.sent())); success.setValue(Long.toString(snapshot.successful()));
        failed.setValue(Long.toString(snapshot.failed() + snapshot.limited())); rate.setValue(String.format("%.1f", snapshot.currentRate()));
        response.setValue(String.format("%.1f ms", snapshot.averageResponseMs()));
        elapsed.setValue(format(snapshot.elapsedSeconds())); remaining.setValue(format(snapshot.remainingSeconds()));
    }

    private String format(long seconds) { return "%02d:%02d".formatted(seconds / 60, seconds % 60); }
}
