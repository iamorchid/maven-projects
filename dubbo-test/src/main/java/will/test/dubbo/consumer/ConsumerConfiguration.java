package will.test.dubbo.consumer;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
// for dubbo to resolve @Reference
@EnableDubbo(scanBasePackages = "will.test.dubbo.consumer.action")
@PropertySource("classpath:/spring/dubbo-consumer.properties")
// for spring to auto scan bean
@ComponentScan(value = {"will.test.dubbo.consumer.action"})
public class ConsumerConfiguration {

}
