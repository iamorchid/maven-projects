package will.test.redis;

public interface IRedisClient {
    RedisResponse<Boolean> setnx(String key, String value, int expireSeconds);

    RedisResponse<Boolean> expire(String key, int seconds);

    RedisResponse<String> get(String key);
}
