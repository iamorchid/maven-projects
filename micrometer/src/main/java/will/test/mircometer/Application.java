package will.test.mircometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        Metrics.globalRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                System.out.println("id: " + id);
                return MeterFilterReply.ACCEPT;
            }
        });

        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.run(args);
        System.out.println("application started");
    }

}
