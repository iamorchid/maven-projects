package will.test.dubbo.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import will.test.dubbo.consumer.action.ConsumerAction;

@Slf4j
public class ConsumerBootstrap {
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        context.start();

        ConsumerAction action = context.getBean(ConsumerAction.class);
        log.info("books: {}", action.getBooks());

        log.info("dubbo service started.");
        System.in.read();
    }
}
