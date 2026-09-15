package client.view;

import client.model.TrafficStatistics;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Six compact cards tailored to the single-source DoS screen. */
public final class DosStatisticsPanel extends GridPane {
    private final MetricCard sent = new MetricCard(CyberIcon.Type.SEND, "Requests Sent", "metric-blue");
    private final MetricCard success = new MetricCard(CyberIcon.Type.CHECK_CIRCLE, "Successful", "metric-green");
    private final MetricCard failed = new MetricCard(CyberIcon.Type.WARNING, "Failed / Limited", "metric-red");
    private final MetricCard rate = new MetricCard(CyberIcon.Type.CHART, "Current RPS", "metric-purple");
    private final MetricCard response = new MetricCard(CyberIcon.Type.CLOCK, "Avg Response Time", "metric-yellow");
    private final MetricCard elapsed = new MetricCard(CyberIcon.Type.CLOCK, "Elapsed Time", "metric-cyan");

    public DosStatisticsPanel() {
        setHgap(9);
        for (int i = 0; i < 6; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(16.666);
            column.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(column);
        }
        add(sent, 0, 0); add(success, 1, 0); add(failed, 2, 0);
        add(rate, 3, 0); add(response, 4, 0); add(elapsed, 5, 0);
    }

    public void show(TrafficStatistics.Snapshot snapshot) {
        if (snapshot == null) {
            sent.setValue("0"); success.setValue("0"); failed.setValue("0");
            rate.setValue("0"); response.setValue("0 ms"); elapsed.setValue("00:00");
            return;
        }
        sent.setValue(Long.toString(snapshot.sent()));
        success.setValue(Long.toString(snapshot.successful()));
        failed.setValue(Long.toString(snapshot.failed() + snapshot.limited()));
        rate.setValue(String.format("%.1f", snapshot.currentRate()));
        response.setValue(String.format("%.1f ms", snapshot.averageResponseMs()));
        elapsed.setValue("%02d:%02d".formatted(snapshot.elapsedSeconds() / 60, snapshot.elapsedSeconds() % 60));
    }
}
