package will.test.mircometer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class SimpleController {

    static final AtomicInteger GUAGE = Metrics.gauge("gauge number", new AtomicInteger(0));

    static final Counter GetNameRequestCounter = Metrics.counter("request_counter", "path", "getName");
    static final Timer GetNameTimer = Timer
            .builder("duration timer")
            .tag("path", "getName")
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofMillis(3000))
            .register(Metrics.globalRegistry);

    static final Counter GetNameWatingRequestCounter = Metrics.counter("request_counter", "path", "getNameWaiting");
    static final Timer GetNameWatingTimer = Timer
            .builder("duration timer")
            .tag("path", "getNameWaiting")
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofMillis(3000))
            .register(Metrics.globalRegistry);

    static final DistributionSummary summary = DistributionSummary
            .builder("distribution")
            .description("simple distribution summary")
            .minimumExpectedValue(10L)
            .maximumExpectedValue(3000L)
            .publishPercentileHistogram()
            .register(Metrics.globalRegistry);

    @RequestMapping("/getName")
    public String getName() {
        GetNameRequestCounter.increment();
        int duration = new Random().nextInt(1000);
        GetNameTimer.record(Duration.ofMillis(duration));
        summary.record(duration);
        GUAGE.set(new Random().nextInt(5));
        return "prometheus-demo";
    }

    @RequestMapping("/getNameWaiting")
    public String getNameWaiting() {
        GetNameWatingRequestCounter.increment();
        int duration = new Random().nextInt(5000);
        GetNameWatingTimer.record(Duration.ofMillis(duration));
        summary.record(duration);
        GUAGE.set(new Random().nextInt(5));
        return "prometheus-demo";
    }
}
