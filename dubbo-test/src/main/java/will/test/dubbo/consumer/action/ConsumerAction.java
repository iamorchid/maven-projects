package will.test.dubbo.consumer.action;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import will.test.dubbo.service.api.IBookStore;

import java.util.List;

@Component
public class ConsumerAction {

    @DubboReference(interfaceClass = IBookStore.class, group = "will", version = "1.0.0", timeout = 1000)
    private IBookStore bookStore;

    public List<String> getBooks() {
        return bookStore.books();
    }
}
