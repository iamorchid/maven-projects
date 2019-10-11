package will.tests.comm.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.context.NettyContextResolver;
import will.tests.comm.core.pipeline.PipelineInitializer;

public class ServerManager extends ChannelManager<ServerChannel> {
    private static final Logger LOG = LoggerFactory.getLogger(ServerManager.class);

    private final ServerBootstrap bootstrap;
    private final int port;
    private Channel serverChannel;

    // Use the same event loop group for both boss and worker. And the group
    // would have default number of threads assigned (2 * cores).
    public ServerManager(int port, final PipelineInitializer childPipelineInit) {
        this(port, 0, -1, childPipelineInit);
    }

    public ServerManager(int port, int bossThreads, int workerThreads, final PipelineInitializer childPipelineInit) {
        super(NettyContextResolver.createServerContext(bossThreads, workerThreads));

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
                                LOG.info("client channel {} became inactive", ch);
                                super.channelInactive(ctx);
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 512)
                .option(ChannelOption.SO_REUSEADDR, true);
        this.port = port;
    }

    public ChannelFuture start() {
        ChannelFuture future = bootstrap.bind(port);
        future.addListener((ChannelFutureListener) future1 -> {
            if (future1.cause() == null) {
                serverChannel = future1.channel();
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
        context.shutdown();
    }
}
