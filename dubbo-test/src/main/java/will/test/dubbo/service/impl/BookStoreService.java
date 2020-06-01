package will.test.dubbo.service.impl;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Method;
import will.test.dubbo.service.api.IBookStore;

import java.util.Arrays;
import java.util.List;

@DubboService(
        version = "1.0.0",
        group = "will",
        parameters = {"param1", "key1", "param2", "key2"},
        methods = {
                @Method(name = "books", timeout = 5000, retries = 3)
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
