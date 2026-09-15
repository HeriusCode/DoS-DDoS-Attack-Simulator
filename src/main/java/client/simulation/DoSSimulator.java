package client.simulation;

import client.model.*;
import java.util.concurrent.*;

public class DoSSimulator {
    protected ScheduledExecutorService executor; protected ScheduledFuture<?>[] workers;
    public void start(TargetServer target, SimulationConfig config, TrafficStatistics stats) {
        executor = Executors.newScheduledThreadPool(config.nodes()); workers = new ScheduledFuture<?>[config.nodes()];
        long periodMs = 1000L / config.requestsPerSecondPerNode(); stats.activeWorkers(config.nodes());
        for (int i=0;i<config.nodes();i++) { stats.registerWorker(i+1); workers[i]=executor.scheduleAtFixedRate(new SimulationWorker(i+1,target,stats),0,periodMs,TimeUnit.MILLISECONDS); }
    }
    public void stop(TrafficStatistics stats) { if (workers != null) for (var f:workers) f.cancel(true); if (executor != null) executor.shutdownNow(); stats.activeWorkers(0); }
}
