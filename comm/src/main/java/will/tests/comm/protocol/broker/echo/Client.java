package will.tests.comm.protocol.broker.echo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ClientManager;
import will.tests.comm.core.EndPoint;
import will.tests.comm.core.pipeline.ProtoBufRWPipelineInitializer;
import will.tests.comm.protocol.broker.BrokerMessageWrapper;

import static will.tests.comm.protocol.broker.BrokerMessageWrapper.*;

@SuppressWarnings("unchecked")
public class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        ClientManager manager = new ClientManager(0, new ProtoBufRWPipelineInitializer<>(
                BrokerMessageWrapper.BrokerMessage.getDefaultInstance(),
                () -> new SimpleChannelInboundHandler<BrokerMessage>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, BrokerMessageWrapper.BrokerMessage msg) {
                        System.out.println("received echo: \n" + msg);
                    }
                }
        ));
        EndPoint address = new EndPoint("10.5.66.37", 33333);

        sendMessage(manager, address);

        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            //
        }

        manager.closeEndpoint(address);


        sendMessage(manager, address);

        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            //
        }

        manager.closeEndpoint(address);

        manager.shutdown();
    }


    private static void sendMessage(ClientManager manager, EndPoint address) {
        for (int i = 0; i < 5; i++) {
            BrokerMessage.Builder builder = BrokerMessage.newBuilder()
                    .setMessageType(MessageType.DISCONNECT)
                    .setSource("source")
                    .setTarget("target");

            if (i % 2 == 0) {
                builder.setDisconnectMsg(
                        DisconnectPack.newBuilder()
                                .setDeviceKey("deviceKey: " + i)
                                .setProductKey("productKey: " + i)
                                .build()
                );
            } else {
                builder.setDownstreamMsg(DownstreamPack.newBuilder()
                        .setContent("test: " + i)
                        .build());
            }


            new Thread(() -> {
                manager.sendMessage(address, builder.build()).addListener(future -> {
                    if (future.cause() == null) {
                        LOG.info("sent message");
                    } else {
                        LOG.info("failed to send message: {}", future.cause().getMessage());
                    }
                });
            }).start();
        }
    }
}
