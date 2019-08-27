package will.tests.comm.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.pipeline.PipelineInitializer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class that manages all local client channels.
 *
 * @author jian.zhang4
 */
public class ClientManager extends ChannelManager<Channel> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientManager.class);

    private static final AttributeKey<Endpoint> ATTR_ENDPOINT = AttributeKey.valueOf("ENDPOINT");

    private final Map<Endpoint, Channel> clients = new ConcurrentHashMap<>();
    private final Map<Endpoint, ChannelFuture> pendingClients = new ConcurrentHashMap<>();
    private final Bootstrap bootstrap;

    public ClientManager(int nThreads, final PipelineInitializer pipelineInit) {
        this(nThreads, 5000, pipelineInit);
    }

    public ClientManager(int nThreads, int connTimeoutMS, final PipelineInitializer pipelineInit) {
        super(NettyContextResolver.createClientContext(true, nThreads));

        bootstrap = new Bootstrap()
                .group(context.getLoopGroup())
                .channel(context.getChannelClass())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        pipelineInit.init(ch.pipeline());

                        ch.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);

                                Endpoint endpoint = ctx.channel().attr(ATTR_ENDPOINT).get();

                                // This could happen if we re-use the existing channel.
                                if (endpoint != null) {
                                    LOG.info("Endpoint {} became inactive", endpoint);
                                    clients.remove(endpoint);
                                }
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connTimeoutMS);
    }

    public void shutdown() {
        clients.values().forEach(channel -> {
            try {
                channel.close().sync();
            } catch (Exception e) {
                // ignore this
            }
        });
        context.getLoopGroup().shutdownGracefully();
    }

    public void closeEndpoint(Endpoint address) {
        Channel channel = clients.remove(address);
        if (channel == null) {
            LOG.warn("failed to close endpoint {} as it doesn't exist", address);
        } else {
            channel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.cause() == null) {
                        LOG.info("closed endpoint {}", address);
                    } else {
                        LOG.info("failed to close endpoint {}", address, future.cause());
                    }
                }
            });
        }
    }

    public Promise<Boolean> sendMessage(Endpoint address, final Object data) {
        final DefaultPromise<Boolean> promise = new DefaultPromise<>(context.getLoopGroup().next());

        Channel channel = clients.get(address);
        if (channel == null) {
            final ChannelFuture future = pendingClients.computeIfAbsent(address, endp -> {
                LOG.info("Now create new channel for endpoint {}", endp);
                return createChannel(endp);
            });

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture connFuture) {
                    // Set up the channel for the end-point first
                    setupEndpointChannel(connFuture, address);

                    Channel channel = clients.get(address);
                    if (channel == null) {
                        if (connFuture.cause() == null) {
                            LOG.error("[BUG] channel is null but no exception caused is not defined");
                            promise.setFailure(new IllegalStateException("[BUG] failure cause expected"));
                        } else {
                            promise.setFailure(connFuture.cause());
                        }
                    } else {
                        doSendMessage(channel, data, promise);
                    }
                }
            });
        } else {
            doSendMessage(channel, data, promise);
        }

        return promise;
    }

    private void doSendMessage(Channel channel, Object data, final DefaultPromise<Boolean> promise) {
        channel.writeAndFlush(data).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture sendFuture) {
                if (sendFuture.cause() != null) {
                    promise.setFailure(sendFuture.cause());
                } else {
                    promise.setSuccess(Boolean.TRUE);
                }
            }
        });
    }

    /**
     * Be careful about the logic here. It's easy to introduce bugs here.
     * @param connFuture
     * @param address
     */
    private synchronized void setupEndpointChannel(ChannelFuture connFuture, Endpoint address) {
        Channel seenChannel = clients.get(address);

        if (connFuture.cause() != null) {
            LOG.error("failed to create new channel for {}", address, connFuture.cause());
            if (seenChannel != null) {
                LOG.error("would re-use existing channel {} for {}", seenChannel, address);
            }
        } else {
            if (seenChannel == null || seenChannel != connFuture.channel()) {
                LOG.info("successfully created new channel {} for {}", connFuture.channel(), address);
            }

            if (seenChannel != null && seenChannel != connFuture.channel()) {
                // We should re-use the existing channel
                LOG.warn("close the duplicated channel {} and re-use the channel {} for {}",
                        connFuture.channel(), seenChannel, address);
                connFuture.channel().close();
            }
        }

        // bind the channel if this is the first channel we see for the endpoint
        if (seenChannel == null && connFuture.cause() == null) {
            LOG.info("bind new channel {} to {}", connFuture.channel(), address);
            connFuture.channel().attr(ATTR_ENDPOINT).set(address);
            clients.put(address, connFuture.channel());
        }

        // We need to handle this after we populate clients
        pendingClients.remove(address);
    }

    /**
     * This is only package visible for {@link HostPublicIpResolver}
     */
    ChannelFuture createChannel(final Endpoint address) {
        return bootstrap.connect(new InetSocketAddress(address.getHost(), address.getPort()));
    }

}
