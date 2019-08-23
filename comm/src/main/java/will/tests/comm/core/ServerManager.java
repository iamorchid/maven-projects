package will.tests.comm.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.pipeline.PipelineInitializer;

public class ServerManager extends ChannelManager<ServerChannel> {
    private static final Logger LOG = LoggerFactory.getLogger(ServerManager.class);

    private final ServerBootstrap bootstrap;
    private final int port;
    private Channel serverChannel;

    public ServerManager(int port, int nThreads, final PipelineInitializer childPipelineInit) {
        super(NettyContextResolver.createServerContext(true, nThreads));

        this.bootstrap = new ServerBootstrap()
                .group(context.getLoopGroup())
                .channel(context.getChannelClass())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        LOG.info("now init a new accepted channel {}", ch);

                        childPipelineInit.init(ch.pipeline());

                        // Do logging when a client disconnects
                        ch.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                LOG.info("channel {} became inactive", ch);
                                super.channelInactive(ctx);
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 512)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        this.port = port;
    }

    public ChannelFuture start() {
        ChannelFuture future = bootstrap.bind(port);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.cause() == null) {
                    serverChannel = future.channel();
                }
            }
        });
        return future;
    }

    public void shutdown() {
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
            } catch (Exception e) {
                // ignore this
            }
        }
        context.getLoopGroup().shutdownGracefully();
    }
}
