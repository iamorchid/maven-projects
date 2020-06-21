package will.test.dubbo.service.api;

import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IBookStore {

    /**
     * provider: {@link AbstractProxyInvoker#wrapWithFuture(Object)}
     * consumer: {@link AsyncRpcResult#recreate()} for InvokeMode.FUTURE
     * @return
     * @see <href>https://www.cnblogs.com/tlj2018/articles/11676384.html</href>
     */
    CompletableFuture<List<String>> books();

    List<String> getBestSellers();
}
