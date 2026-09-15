package client.simulation;

import client.model.*;
import client.network.HttpClient;

final class SimulationWorker implements Runnable {
    private final int workerId; private final TargetServer target; private final TrafficStatistics stats; private final HttpClient http = new HttpClient();
    SimulationWorker(int workerId, TargetServer target, TrafficStatistics stats) { this.workerId=workerId; this.target=target; this.stats=stats; }
    public void run() {
        if (Thread.currentThread().isInterrupted()) return;
        stats.sent(workerId); long start=System.nanoTime();
        try { int status=http.get(target); if (status >= 200 && status < 400) stats.success(workerId,(System.nanoTime()-start)/1_000_000); else stats.failed(workerId,status==429 || status==503); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); stats.failed(workerId,false); }
        catch (Exception e) { stats.failed(workerId,false); }
    }
}
