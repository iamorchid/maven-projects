package will.tests.comm.core;

import com.google.common.base.Preconditions;

import java.util.Objects;

public class EndPoint {
    private final String host;
    private final int port;

    public EndPoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public EndPoint(String hostAndPort) {
        String[] hostPort = hostAndPort.split("[:]");
        Preconditions.checkArgument(hostPort.length == 2, "invalid host and port: " + hostAndPort);

        this.host = hostPort[0];
        this.port = Integer.parseInt(hostPort[1]);
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
        EndPoint endpoint = (EndPoint) o;
        return port == endpoint.port &&
                Objects.equals(host, endpoint.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
