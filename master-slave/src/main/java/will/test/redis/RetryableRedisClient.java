package will.test.redis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RetryableRedisClient implements IRedisClient {
    private static final Logger LOG = LogManager.getLogger(RetryableRedisClient.class);
    public static final int MAX_RETRIES = 3;
    public static final IRedisClient SINGLETON = new RetryableRedisClient();

    private volatile IRedisClient underlying;

    @Override
    public RedisResponse<Boolean> setnx(String key, String value, int expireSeconds) {
        return doRun(() -> getUnderlying().setnx(key, value, expireSeconds), MAX_RETRIES);
    }

    @Override
    public RedisResponse<Boolean> expire(final String key, final int seconds) {
        return doRun(() -> getUnderlying().expire(key, seconds), MAX_RETRIES);
    }

    @Override
    public RedisResponse<String> get(String key) {
        return doRun(() -> getUnderlying().get(key), MAX_RETRIES);
    }

    private <T> RedisResponse<T> doRun(final Supplier<RedisResponse<T>> task, final int retries) {
        RedisResponse<T> result;
        try {
            result = task.get();
        } catch (Throwable e) {
            LOG.error("failed to execute redis task, left retries [{}], error: {}", retries, e.getMessage());
            result = RedisResponse.fail();
        }

        if (result.isOK()) {
            return result;
        }

        if (retries == 0) {
            LOG.error("exhausted all retries");
            return RedisResponse.fail();
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable error) {
            // ignore this
        }

        // Perform the retry
        return doRun(task, retries - 1);
    }

    @SneakyThrows
    private IRedisClient getUnderlying() {
        if (underlying == null) {
            synchronized (this) {
                underlying = new RedisClient();
            }
        }
        return underlying;
    }
}
