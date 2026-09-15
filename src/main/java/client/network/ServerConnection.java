package client.network;

import client.model.TargetServer;
public final class ServerConnection {
    private final HttpClient client = new HttpClient();
    public boolean test(TargetServer target) {
        target.validateLabTarget();
        try { return client.get(target) < 500; } catch (Exception e) { return false; }
    }
}
