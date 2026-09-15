package client.controller;

import client.logging.SimulationLogger;
import client.model.*;
import client.simulation.*;
import java.util.concurrent.*;

public final class AttackController {
    private volatile SimulationState state = SimulationState.STOPPED;
    private DoSSimulator simulator; private TrafficStatistics statistics; private SimulationConfig config;
    private ScheduledExecutorService timer;
    public synchronized void start(TargetServer target, SimulationConfig cfg, boolean ddos, Runnable onComplete) {
        if (state == SimulationState.RUNNING) throw new IllegalStateException("A simulation is already running");
        target.validateLabTarget(); config=cfg; statistics=new TrafficStatistics(); simulator=ddos ? new DDoSSimulator() : new DoSSimulator(); state=SimulationState.RUNNING;
        SimulationLogger.info("Simulation started: " + target.uri() + ", nodes=" + cfg.nodes() + ", rate/node=" + cfg.requestsPerSecondPerNode());
        simulator.start(target,cfg,statistics);
        timer=Executors.newSingleThreadScheduledExecutor();
        timer.schedule(() -> { synchronized (this) { if (state==SimulationState.RUNNING) { simulator.stop(statistics); state=SimulationState.COMPLETED; SimulationLogger.info("Simulation completed"); onComplete.run(); } } },cfg.durationSeconds(),TimeUnit.SECONDS);
    }
    public synchronized void stop() { if (state == SimulationState.RUNNING) { simulator.stop(statistics); if(timer!=null) timer.shutdownNow(); state=SimulationState.STOPPED; SimulationLogger.info("Simulation stopped"); } }
    public SimulationState state() { return state; }
    public TrafficStatistics.Snapshot snapshot() { return statistics == null ? null : statistics.snapshot(config.durationSeconds()); }
}
