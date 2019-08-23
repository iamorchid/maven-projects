package will.tests.comm.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.pipeline.PipelineInitializer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager extends ChannelManager<Channel> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientManager.class);

    private final Map<Endpoint, Channel> clients = new ConcurrentHashMap<>();
    private final Bootstrap bootstrap;

    public ClientManager(int nThreads, final PipelineInitializer pipelineInit) {
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
                                LOG.info("channel {} became inactive", ch);
                                super.channelInactive(ctx);
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true);
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
            LOG.error("failed to close endpoint {} as it doesn't exist", address);
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
        if (channel == null || !channel.isActive()) {
            if (channel != null) {
                LOG.warn("close inactive channel {} for address {}", channel, address);
                channel.close();
            }

            createChannel(address).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture connFuture) {
                    if (connFuture.cause() != null) {
                        promise.setFailure(connFuture.cause());
                    } else {
                        clients.put(address, connFuture.channel());
                        doSendMessage(connFuture.channel(), data, promise);
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

    private ChannelFuture createChannel(Endpoint address) {
        LOG.info("Now create new channel for {}", address);
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(address.getIpAddr(), address.getPort()));

        future.addListener(connFuture -> {
            if (connFuture.cause() != null) {
                LOG.error("failed to create new channel for {}", address, connFuture.cause());
            } else {
                LOG.info("successfully created new channel for {}", address);
            }
        });

        return future;
    }

}
