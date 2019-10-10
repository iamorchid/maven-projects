package will.tests.comm.core;

import com.envision.eos.commons.utils.LionUtil;
import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class HostPublicIpResolver {
    private static final Logger LOG = LoggerFactory.getLogger(HostPublicIpResolver.class);

    private static final List<String> DEFAULT_KEYS = ImmutableList.of(
            "Camel.kafka-common", // kafka server list
            "Bull.eos-cloud.addr" // redis server list
    );

    public static List<EndPoint> getDefaultWellKnownServers() {
        List<EndPoint> r = new LinkedList<>();

        for (String key : DEFAULT_KEYS) {
            String config = LionUtil.getStringValue(key, "");

            if (StringUtils.isBlank(config)) {
                LOG.error("found no config value for lion key {}", key);
            } else {
                r.addAll(parse(key, config));
            }
        }

        return r;
    }

    private static List<EndPoint> parse(String key, String config) {
        String[] values = config.split("[,;\\s]");
        if (ArrayUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        List<EndPoint> r = new LinkedList<>();
        for(String value : values) {
            if (StringUtils.isBlank(value)) {
                continue;
            }

            String[] hostPort = value.split("[:]");
            if (ArrayUtils.isNotEmpty(hostPort) && hostPort.length == 2) {
                try {
                    r.add(new EndPoint(hostPort[0].trim(), Integer.parseInt(hostPort[1].trim())));
                } catch (Exception e) {
                    LOG.error("unable to parse value {} from lion key {}", hostPort, key);
                }
            } else {
                LOG.error("unable to parse value {} from lion key {}", hostPort, key);
            }
        }

        return r;
    }

    public static String getPublicIp() {
        InetAddress address = getPublicIp(getDefaultWellKnownServers());
        if (address == null) {
            throw new RuntimeException("failed to resolve public ip");
        }
        return address.getHostAddress();
    }

    /**
     * A list of public servers that are used to resolve our local public ip.
     * This is a more reliable way to resolve our local public ip than the method
     * {@link HostPublicIpResolver#getPublicIpAddr()}.
     *
     * @param publicServers a list of public servers
     * @return null if public ip is not available
     */
    public static InetAddress getPublicIp(List<EndPoint> publicServers) {

        ClientManager manager = new ClientManager(1, 3000, pipeline -> {
            // we don't need this actually
        });

        final MutableObject obj = new MutableObject();

        try {
            for (EndPoint server : publicServers) {
                final CountDownLatch latch = new CountDownLatch(1);
                manager.createChannel(server).addListener(
                        new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture connFuture) throws Exception {
                                if (connFuture.cause() == null) {
                                    InetAddress address = ((SocketChannel) connFuture.channel()).localAddress().getAddress();
                                    if (!address.isLoopbackAddress()) {
                                        LOG.warn("found non-loopback address: {}", address.getHostAddress());
                                        obj.setValue(address);
                                    }
                                }
                                latch.countDown();
                            }
                        }
                );

                latch.await();
                if (obj.getValue() != null) {
                    LOG.info("found available public ip {} through public server {}", obj.getValue(), server);
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("ignored error", e);
        } finally {
            manager.shutdown();
        }

        if (obj.getValue() != null) {
            return (InetAddress)obj.getValue();
        }

        return getPublicIpAddr();
    }


    /**
     * {@link HostPublicIpResolver#getPublicIp(List)} should be preferred to this method. Use this
     * method when you don't have public servers available.
     *
     * @return null if public ip is not available
     */
    public static InetAddress getPublicIpAddr() {
        try {
            // Traversal Network interface to get the first non-loopback and non-private address
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            List<InetAddress> ipv4Result = new LinkedList<>();
            List<InetAddress> ipv6Result = new LinkedList<>();
            while (enumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = enumeration.nextElement();
                if (networkInterface.getName().contains("docker")) {
                    continue;
                }

                final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()) {
                    final InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet6Address) {
                            ipv6Result.add(address);
                        } else {
                            ipv4Result.add(address);
                        }
                    }
                }
            }

            // prefer ipv4
            for (InetAddress ip : ipv4Result) {
                if (ip.getHostAddress().startsWith("127.") || ip.getHostAddress().startsWith("192.168")) {
                    continue;
                }
                return ip;
            }

            for (InetAddress ip : ipv6Result) {
                if (ip.getHostAddress().equals("::1")) {
                    continue;
                }
                return ip;
            }

            // If failed to find,fall back to localhost
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            LOG.error("Failed to obtain local address", e);
        }

        return null;
    }

    public static void main(String[] args) {
        System.out.println(parse("Bull.eos-cloud.addr",
                "10.27.20.121:7000;10.27.20.121:7001;10.27.20.122:7000;10.27.20.122:7001;10.27.20.123:7000;10.27.20.123:7001"));
        System.out.println(parse("Camel.kafka-common", "kafka9001.eniot.io:9092,kafka9002.eniot.io:9092"));
        System.out.println(parse("Camel.kafka-common", "kafka9001.eniot.io:9092   kafka9002.eniot.io:9092"));

        System.out.println(getDefaultWellKnownServers());

        System.out.println(getPublicIp(getDefaultWellKnownServers()));
    }
}
