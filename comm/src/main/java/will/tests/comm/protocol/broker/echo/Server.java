package will.tests.comm.protocol.broker.echo;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ServerManager;
import will.tests.comm.core.pipeline.ProtoBufRWPipelineInitializer;
import will.tests.comm.protocol.broker.BrokerMessageWrapper;

public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        ServerManager manager = new ServerManager(33333,
                new ProtoBufRWPipelineInitializer<>(
                        BrokerMessageWrapper.BrokerMessage.getDefaultInstance(),
                        () -> new SimpleChannelInboundHandler<BrokerMessageWrapper.BrokerMessage>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, BrokerMessageWrapper.BrokerMessage msg) {
                                System.out.println("received: \n" + msg);
                                // TODO: think that why ctx.writeAndFlush(msg) fails here ???
                                // I finally understood it. How about you ? Make sure you understand how netty pipeline works.
                                ctx.channel().writeAndFlush(msg).addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) {
                                        if (future.cause() == null) {
                                            LOG.info("sent echo to {}", future.channel());
                                        } else {
                                            LOG.info("failed to send echo to {}", future.channel(), future.cause());
                                            future.channel().close();
                                        }
                                    }
                                });
                            }
                        }
                ));
        manager.start().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.cause() == null) {
                    LOG.info("server has successfully started on {}", future.channel());
                } else {
                    LOG.error("server failed to start", future.cause());
                }
            }
        });
    }

}
