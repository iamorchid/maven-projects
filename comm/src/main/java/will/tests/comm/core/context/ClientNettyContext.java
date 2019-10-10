package will.tests.comm.core.context;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

@SuppressWarnings("unchecked")
public class ClientNettyContext extends NettyContext<Channel> {

    public ClientNettyContext(EventLoopGroup loopGroup, Class<?> channelClass) {
        super(loopGroup, (Class<Channel>)channelClass);
    }

}
