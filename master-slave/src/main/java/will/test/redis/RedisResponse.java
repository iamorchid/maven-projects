package will.test.redis;

public class RedisResponse<T> {
    public final static RedisResponse<Boolean> OK = RedisResponse.create(true);
    public final static RedisResponse<Boolean> FAIL = RedisResponse.create(false);

    /**
     * If this is true, it means the redis operation has been done successfully
     */
    private final boolean redisOk;

    private final T result;

    public RedisResponse(boolean redisOk) {
        this(redisOk, null);
    }

    public RedisResponse(boolean redisOk, T data) {
        this.redisOk = redisOk;
        this.result = data;
    }

    public boolean isOK() {
        return redisOk;
    }

    public boolean hasResult() {
        return result != null;
    }

    public T getResult() {
        return result;
    }

    public static <T> RedisResponse<T> create(T result) {
        return new RedisResponse<>(true, result);
    }

    public static <T> RedisResponse<T> fail() {
        return new RedisResponse<>(false);
    }
}
