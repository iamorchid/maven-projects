package will.tests.comm.core;

import java.util.Objects;

public class Endpoint {
    private final String ipAddr;
    private final int port;

    public Endpoint(String ipAddr, int host) {
        this.ipAddr = ipAddr;
        this.port = host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(ipAddr, endpoint.ipAddr) &&
                Objects.equals(port, endpoint.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddr, port);
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "ipAddr='" + ipAddr + '\'' +
                ", host='" + port + '\'' +
                '}';
    }
}
