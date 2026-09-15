package client.model;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Aggregate and per-logical-worker counters. All mutations are thread-safe. */
public final class TrafficStatistics {
    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong successful = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong limited = new AtomicLong();
    private final AtomicLong latencyMs = new AtomicLong();
    private final AtomicInteger activeWorkers = new AtomicInteger();
    private final ConcurrentMap<Integer, WorkerCounter> workerCounters = new ConcurrentHashMap<>();
    private final long startedAt = System.nanoTime();

    public void registerWorker(int workerId) {
        workerCounters.putIfAbsent(workerId, new WorkerCounter());
        workerCounters.get(workerId).active.set(true);
    }

    public void sent(int workerId) {
        sent.incrementAndGet();
        workerCounters.computeIfAbsent(workerId, ignored -> new WorkerCounter()).sent.incrementAndGet();
    }

    public void success(int workerId, long responseMs) {
        successful.incrementAndGet();
        latencyMs.addAndGet(responseMs);
        workerCounters.computeIfAbsent(workerId, ignored -> new WorkerCounter()).successful.incrementAndGet();
    }

    public void failed(int workerId, boolean isLimited) {
        (isLimited ? limited : failed).incrementAndGet();
        workerCounters.computeIfAbsent(workerId, ignored -> new WorkerCounter()).failed.incrementAndGet();
    }

    public void activeWorkers(int count) {
        activeWorkers.set(count);
        if (count == 0) workerCounters.values().forEach(counter -> counter.active.set(false));
    }

    public Snapshot snapshot(int duration) {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000_000L;
        long total = sent.get();
        List<WorkerSnapshot> workers = workerCounters.entrySet().stream()
                .map(entry -> {
                    WorkerCounter counter = entry.getValue();
                    long workerSent = counter.sent.get();
                    return new WorkerSnapshot(entry.getKey(), counter.active.get(), workerSent,
                            counter.successful.get(), counter.failed.get(),
                            elapsed == 0 ? 0 : workerSent / (double) elapsed);
                })
                .sorted(Comparator.comparingInt(WorkerSnapshot::workerId))
                .toList();
        return new Snapshot(total, successful.get(), failed.get(), limited.get(), elapsed,
                Math.max(0, duration - elapsed), activeWorkers.get(),
                elapsed == 0 ? 0 : total / (double) elapsed,
                successful.get() == 0 ? 0 : latencyMs.get() / (double) successful.get(), workers);
    }

    private static final class WorkerCounter {
        private final AtomicLong sent = new AtomicLong();
        private final AtomicLong successful = new AtomicLong();
        private final AtomicLong failed = new AtomicLong();
        private final AtomicBoolean active = new AtomicBoolean();
    }

    public record WorkerSnapshot(int workerId, boolean active, long sent, long successful,
                                 long failed, double requestsPerSecond) { }

    public record Snapshot(long sent, long successful, long failed, long limited,
                           long elapsedSeconds, long remainingSeconds, int activeWorkers,
                           double currentRate, double averageResponseMs,
                           List<WorkerSnapshot> workers) { }
}
