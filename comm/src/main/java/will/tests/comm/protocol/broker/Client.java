package will.tests.comm.protocol.broker;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ClientManager;
import will.tests.comm.core.Endpoint;
import will.tests.comm.core.pipeline.ProtocolBufWriteOnlyPipelineInitializer;

import static will.tests.comm.protocol.broker.BrokerMessageWrapper.*;

@SuppressWarnings("unchecked")
public class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        ClientManager manager = new ClientManager(0, new ProtocolBufWriteOnlyPipelineInitializer());

        Endpoint address = new Endpoint("localhost", 33333);
        for (int i = 0; i < 10; i++) {
            BrokerMessage.Builder builder = BrokerMessage.newBuilder()
                   .setMessageType(MessageType.DISCONNECT)
                   .setSourceBroker("source")
                   .setTargetBroker("target");

            if (i % 2 == 0) {
                builder.setDisconnectMsg(
                        DisconnectPack.newBuilder()
                                .setDeviceKey("deviceKey")
                                .setProductKey("productKey")
                                .build()
                );
            } else {
                builder.setDownstreamMsg(DownstreamPack.newBuilder()
                        .setContent("{\"json\": \"test\"}")
                        .build());
            }

            manager.sendMessage(address, builder.build()).addListener(new GenericFutureListener<Future<? super Boolean>>() {
                @Override
                public void operationComplete(Future<? super Boolean> future) throws Exception {
                    if (future.isSuccess()) {
                        LOG.info("sent message");
                    } else {
                        LOG.info("failed to send message");
                    }
                }
            });

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                //
            }
        }

        manager.closeEndpoint(address);

        manager.shutdown();
    }
}
