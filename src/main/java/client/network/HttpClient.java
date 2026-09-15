package client.network;

import client.model.TargetServer;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpClient {
    private final java.net.http.HttpClient delegate = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public int get(TargetServer target) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(target.uri()).GET().timeout(Duration.ofSeconds(3)).header("User-Agent", "Academic-Lab-Simulator/1.0").build();
        return delegate.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
