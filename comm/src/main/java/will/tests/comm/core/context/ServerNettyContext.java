package will.tests.comm.core.context;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

@SuppressWarnings("unchecked")
public class ServerNettyContext extends NettyContext<ServerChannel> {
    private final EventLoopGroup workerGroup;

    public ServerNettyContext(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Class<?> channelClass) {
        super(bossGroup, (Class<ServerChannel>)channelClass);
        this.workerGroup = workerGroup;
    }

    public EventLoopGroup getBossGroup() {
        return getLoopGroup();
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    @Override
    public void shutdown() {
        shutdownLoopGroup(getBossGroup());
        if (getBossGroup() != getWorkerGroup()) {
            shutdownLoopGroup(getWorkerGroup());
        }
    }


}
