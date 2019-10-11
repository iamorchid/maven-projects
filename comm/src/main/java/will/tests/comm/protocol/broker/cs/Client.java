package will.tests.comm.protocol.broker.cs;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ClientManager;
import will.tests.comm.core.EndPoint;
import will.tests.comm.core.pipeline.ProtoBufWritePipelineInitializer;

import static will.tests.comm.protocol.broker.BrokerMessageWrapper.*;

@SuppressWarnings("unchecked")
public class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        ClientManager manager = new ClientManager(0, new ProtoBufWritePipelineInitializer());
        EndPoint address = new EndPoint("localhost", 33333);

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
                manager.sendMessage(address, builder.build()).addListener(new GenericFutureListener<Future<? super Boolean>>() {
                    @Override
                    public void operationComplete(Future<? super Boolean> future) throws Exception {
                        if (future.cause() == null) {
                            LOG.info("sent message");
                        } else {
                            LOG.info("failed to send message: {}", future.cause().getMessage());
                        }
                    }
                });
            }).start();

//            try {
//                Thread.sleep(10000);
//            } catch (Exception e) {
//                //
//            }
        }
    }
}
