package will.tests.comm.core;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

public class NettyContext<T extends Channel> {
    private final EventLoopGroup loopGroup;
    private final Class<T> channelClass;

    public NettyContext(EventLoopGroup loopGroup, Class<T> channelClass) {
        this.loopGroup = loopGroup;
        this.channelClass = channelClass;
    }

    public EventLoopGroup getLoopGroup() {
        return loopGroup;
    }

    public Class<T> getChannelClass() {
        return channelClass;
    }
}
