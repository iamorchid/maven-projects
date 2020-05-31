package will.test.dubbo.service.impl;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Method;
import will.test.dubbo.service.api.IBookStore;

import java.util.Arrays;
import java.util.List;

@DubboService(
        version = "1.0.0",
        group = "will",
//        parameters = "{}",
        methods = {
                @Method(name = "books", timeout = 250, retries = 0)
        }
)
public class BookStoreService implements IBookStore {

    @Override
    public List<String> books() {
        return Arrays.asList("Linux", "Network", "Mysql", "Dubbo", "Spring", "Redis", "Kafka", "Elastic Search", "Netty");
    }

    @Override
    public List<String> getBestSellers() {
        return Arrays.asList("Linux", "Network", "Mysql");
    }

}
