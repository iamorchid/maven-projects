package will.tests.comm.protocol.echo;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.ServerManager;

public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        ServerManager manager = new ServerManager(33333, new ServerPipelineInitializer());
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
