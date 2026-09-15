package client.model;

public record SimulationConfig(int nodes, int requestsPerSecondPerNode, int durationSeconds) {
    public static final int MAX_NODES = 3, MAX_RATE_PER_NODE = 20, MAX_DURATION_SECONDS = 60;
    public SimulationConfig {
        if (nodes < 1 || nodes > MAX_NODES) throw new IllegalArgumentException("Nodes must be 1-" + MAX_NODES);
        if (requestsPerSecondPerNode < 1 || requestsPerSecondPerNode > MAX_RATE_PER_NODE) throw new IllegalArgumentException("Rate/node must be 1-" + MAX_RATE_PER_NODE + " req/s");
        if (durationSeconds < 1 || durationSeconds > MAX_DURATION_SECONDS) throw new IllegalArgumentException("Duration must be 1-" + MAX_DURATION_SECONDS + " seconds");
    }
}
