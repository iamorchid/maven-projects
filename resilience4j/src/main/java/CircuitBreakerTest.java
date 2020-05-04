import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedRunnable;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CircuitBreakerTest {

    public static void main(String[] args) throws Exception {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(5000))
                .slowCallDurationThreshold(Duration.ofMillis(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(5)
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);


        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("ES-log");
        circuitBreaker.getEventPublisher()
                .onError(error -> log.info("onError: " + error))
                .onCallNotPermitted(arg -> log.info("onCallNotPermitted"))
                .onStateTransition(event -> log.info("onStateTransition: {}", event.getStateTransition()));

        AtomicBoolean sleepNeeded = new AtomicBoolean(true);

        AtomicInteger idx = new AtomicInteger(0);
        CheckedRunnable runnable = circuitBreaker.decorateCheckedRunnable(() -> {
            if (sleepNeeded.get()) {
                TimeUnit.MILLISECONDS.sleep(20);
            }
            log.info("processed #{}", idx.getAndIncrement());
        });

        for (int i = 0; i < 20; ++i) {
            try {
                if (circuitBreaker.tryAcquirePermission()) {
                    log.info("acquired permission for #{}", i);
                    runnable.run();
                } else {
                    log.info("failed to acquire permission for #{}", i);
                }
            } catch (Throwable e) {
                log.error("error#1: " + e);
            }
        }

        TimeUnit.SECONDS.sleep(5);

        sleepNeeded.set(false);
        for (int i = 0; i < 20; ++i) {
            try {
                runnable.run();
            } catch (Throwable e) {
                log.error("error#2: " + e);
            }
        }
    }

}
