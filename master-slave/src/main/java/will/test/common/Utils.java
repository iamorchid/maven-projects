package will.test.common;

import com.envision.eos.commons.transport.EndPoint;
import com.envision.eos.commons.transport.context.NettyContextResolver;
import com.envision.eos.commons.transport.context.ServerNettyContext;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import will.test.Config;
import will.test.message.internal.IMessage;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Utils {

    private volatile static ServerNettyContext CTX;

    public static ServerNettyContext getServerCtx(Config config) {
        if (CTX == null) {
            synchronized (Utils.class) {
                if (CTX == null) {
                    CTX = NettyContextResolver.createServerContext(config.getNThreads());
                }
            }
        }
        return CTX;
    }

    public static EndPoint getPeerAddress(Channel channel) {
        InetSocketAddress socketAddress = ((SocketChannel)channel).remoteAddress();
        return new EndPoint(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
    }

    public static <T> CompletableFuture<T> conv(Future<?> channelFuture, T value) {
        CompletableFuture<T> result = new CompletableFuture<>();
        channelFuture.addListener(future -> {
            if (future.cause() != null) {
                result.completeExceptionally(future.cause());
            } else {
                Preconditions.checkState(future.isSuccess(), "expect success if no error happens");
                result.complete(value);
            }
        });
        return result;
    }

    public static <T> CompletableFuture<T> doneWithError(Throwable error) {
        CompletableFuture<T> done = new CompletableFuture<>();
        done.completeExceptionally(error);
        return done;
    }

    public static void safeSleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (Exception e) {
            // ignore
        }
    }

    public static String getType(IMessage message) {
        return message != null ? message.getClass().getSimpleName() : "null";
    }
}
