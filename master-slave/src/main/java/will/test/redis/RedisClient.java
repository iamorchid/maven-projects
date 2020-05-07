package will.test.redis;

import com.envision.arch.bull.client.BullClient;
import com.envision.arch.bull.exception.BullException;
import com.envision.eos.commons.utils.LionUtil;
import lombok.extern.slf4j.Slf4j;
import will.test.util.LionKey;

@Slf4j
public class RedisClient implements IRedisClient {
    private static final String OK = "OK";

    private static final Long ONE = 1L;

    private final BullClient redisClient;

    public RedisClient() throws BullException {
        redisClient = new BullClient(LionUtil.getStringValue(LionKey.BULL_NAME_KEY, "Bull.eos-cloud"));
    }

    @Override
    public RedisResponse<Boolean> setnx(String key, String value, int expireSeconds) {
        try {
            String res = redisClient.set(key, value, "NX", "EX", expireSeconds);
            if (OK.equalsIgnoreCase(res)) {
                return RedisResponse.OK;
            }

            log.error("failed to call setnx for key [{}]", key);
            return RedisResponse.FAIL;
        } catch (Exception e) {
            log.error("failed to call setnx for key [{}], error: {}", key, e);
            return RedisResponse.FAIL;
        }
    }

    @Override
    public RedisResponse<Boolean> expire(String key, int seconds) {
        try {
            Long res = redisClient.expire(key, seconds);
            if (ONE.equals(res)) {
                return RedisResponse.OK;
            }

            log.error("failed to call expire for key [{}]", key);
            return RedisResponse.FAIL;
        } catch (Exception e) {
            log.error("failed to call expire for key [{}], error: {}", key, e);
            return RedisResponse.fail();
        }
    }

    @Override
    public RedisResponse<String> get(String key) {
        try {
            return RedisResponse.create(redisClient.get(key));
        } catch (Exception e) {
            log.error("failed to call get for key [{}], error: {}", key, e);
            return RedisResponse.fail();
        }
    }
}
