package will.tests.comm.protocol.echo;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ClientManager;
import will.tests.comm.core.Endpoint;

public class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        ClientManager manager = new ClientManager(0, new ClientPipelineInitializer());

        Endpoint address = new Endpoint("localhost", 33333);
        for (int i = 0; i < 10; i++) {
            manager.sendMessage(address, "message seq#" + i).addListener(new GenericFutureListener<Future<? super Boolean>>() {
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
    }
}
