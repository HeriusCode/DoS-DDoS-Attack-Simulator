package client.model;

import java.net.InetAddress;
import java.net.URI;

public record TargetServer(String host, int port) {
    public TargetServer { if (host == null || host.isBlank()) throw new IllegalArgumentException("Target IP/host is required"); if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be 1-65535"); }
    public URI uri() { return URI.create("http://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + port + "/"); }
    /** Explicit lab fence: DNS names and public addresses are rejected. */
    public void validateLabTarget() {
        try {
            InetAddress a = InetAddress.getByName(host);
            byte[] b = a.getAddress();
            boolean v4private = b.length == 4 && ((b[0] & 255) == 10 || ((b[0]&255)==172 && (b[1]&255)>=16 && (b[1]&255)<=31) || ((b[0]&255)==192 && (b[1]&255)==168));
            if (!(a.isLoopbackAddress() || v4private)) throw new IllegalArgumentException("Only loopback or RFC1918 private lab targets are allowed");
        } catch (java.net.UnknownHostException e) { throw new IllegalArgumentException("Target cannot be resolved"); }
    }
}
