package client.view;

import client.model.TrafficStatistics;
import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Six responsive metric cards used by the dashboard. */
public final class StatisticsPanel extends GridPane {
    private final MetricCard sent = new MetricCard(CyberIcon.Type.EXPORT, "Total Requests Sent", "metric-blue");
    private final MetricCard success = new MetricCard(CyberIcon.Type.SHIELD, "Successful Requests", "metric-green");
    private final MetricCard failed = new MetricCard(CyberIcon.Type.LIGHTNING, "Failed / Limited", "metric-red");
    private final MetricCard rate = new MetricCard(CyberIcon.Type.CHART, "Current Requests/sec", "metric-purple");
    private final MetricCard response = new MetricCard(CyberIcon.Type.TARGET, "Average Response Time", "metric-yellow");
    private final MetricCard workers = new MetricCard(CyberIcon.Type.NETWORK, "Active Workers", "metric-cyan");

    public StatisticsPanel() {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(2));
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.333);
            column.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(column);
        }
        add(sent, 0, 0); add(success, 1, 0); add(failed, 2, 0);
        add(rate, 0, 1); add(response, 1, 1); add(workers, 2, 1);
    }

    public void show(TrafficStatistics.Snapshot snapshot) {
        if (snapshot == null) {
            sent.setValue("0"); success.setValue("0"); failed.setValue("0");
            rate.setValue("0"); response.setValue("0 ms"); workers.setValue("0");
            return;
        }
        sent.setValue(Long.toString(snapshot.sent()));
        success.setValue(Long.toString(snapshot.successful()));
        failed.setValue(Long.toString(snapshot.failed() + snapshot.limited()));
        rate.setValue(String.format("%.1f", snapshot.currentRate()));
        response.setValue(String.format("%.1f ms", snapshot.averageResponseMs()));
        workers.setValue(Integer.toString(snapshot.activeWorkers()));
    }
}
