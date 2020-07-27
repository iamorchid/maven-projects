package will.test.dubbo.consumer.action;

import lombok.SneakyThrows;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import will.test.dubbo.service.api.IBookStore;

import java.util.List;

@Component
public class ConsumerAction {

    @DubboReference(
            interfaceClass = IBookStore.class,
            group = "will",
            version = "1.0.0",
            timeout = 120000,
            parameters = {"param1", "key1", "param2", "consumer-key2"})
    private IBookStore bookStore;

    @SneakyThrows
    public List<String> getBooks() {
        return bookStore.books().get();
    }
}
