package will.tests.comm.core;

import java.util.Objects;

public class Endpoint {
    private final String host;
    private final int port;

    public Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return port == endpoint.port &&
                Objects.equals(host, endpoint.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "{" + "host='" + host + '\'' + ", port=" + port + '}';
    }
}
