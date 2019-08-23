package will.tests.comm.protocol.broker;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ServerManager;
import will.tests.comm.core.pipeline.ProtocolBufReadWritePipelineInitializer;

public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        ServerManager manager = new ServerManager(33333, 0,
                new ProtocolBufReadWritePipelineInitializer<>(
                        BrokerMessageWrapper.BrokerMessage::getDefaultInstance,
                        () -> new SimpleChannelInboundHandler<BrokerMessageWrapper.BrokerMessage>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, BrokerMessageWrapper.BrokerMessage msg) {
                                System.out.println("received: \n" + msg);
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
