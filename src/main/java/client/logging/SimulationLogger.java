package client.logging;

import java.util.logging.Logger;
public final class SimulationLogger {
    private static final Logger LOG = Logger.getLogger("LabSimulation");
    private SimulationLogger() {}
    public static void info(String m) { LOG.info(m); }
    public static void error(String m, Exception e) { LOG.warning(m + ": " + e.getMessage()); }
}
