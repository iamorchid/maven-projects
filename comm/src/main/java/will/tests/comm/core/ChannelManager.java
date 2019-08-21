package will.tests.comm.core;

import io.netty.channel.Channel;

public abstract class ChannelManager<T extends Channel> {
    protected final NettyContext<T> context;

    public ChannelManager(NettyContext<T> context) {
        this.context = context;
    }
}
