package client.model;

public record SimulationConfig(int nodes, int requestsPerSecondPerNode, int durationSeconds) {
    public SimulationConfig {
        if (nodes < 1) throw new IllegalArgumentException("Nodes must be at least 1");
        if (requestsPerSecondPerNode < 1) throw new IllegalArgumentException("Rate/node must be at least 1 req/s");
        if (durationSeconds < 1) throw new IllegalArgumentException("Duration must be at least 1 second");
    }
}
