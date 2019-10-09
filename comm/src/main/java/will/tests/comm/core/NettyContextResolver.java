package will.tests.comm.core;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.function.Function;

public class NettyContextResolver {
    private static final Logger LOG = LoggerFactory.getLogger(NettyContextResolver.class);

    public static NettyContext<ServerChannel> createServerContext(boolean tryEpoll, int nThreads) {
        return createContext(tryEpoll, nThreads, useEpoll -> {
            return useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
        });
    }

    public static NettyContext<Channel> createClientContext(boolean tryEpoll, int nThreads) {
        return createContext(tryEpoll, nThreads, useEpoll -> {
            return useEpoll ? EpollSocketChannel.class : NioSocketChannel.class;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Channel> NettyContext<T> createContext(boolean tryEpoll, int nThreads, Function<Boolean, Class> func) {
        if (isUsingLinuxAndAMD64() && tryEpoll) {
            try {
                LOG.info("trying to use epoll for netty in Linux ...");

                EventLoopGroup group = new EpollEventLoopGroup(nThreads);
                Class<T> channelClass = func.apply(true);

                LOG.info("initialized epoll successfully");
                return new NettyContext<>(group, channelClass);
            } catch (Throwable e) {
                LOG.info("failed to init epoll", e);
            }
        }

        EventLoopGroup group = new NioEventLoopGroup(nThreads);
        Class<T> channelClass = func.apply(false);
        return new NettyContext<>(group, channelClass);
    }

    private static boolean isUsingLinuxAndAMD64() {
        String osName = SystemPropertyUtil.get("os.name").toLowerCase(Locale.ENGLISH).trim();
        String arch = SystemPropertyUtil.get("os.arch").toLowerCase(Locale.ENGLISH).trim();
        return osName.contains("linux") && arch.contains("amd64");
    }
}
