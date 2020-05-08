package will.test.lock.redis;

import com.envision.arch.bull.client.BullClient;
import com.envision.arch.bull.exception.BullException;
import com.envision.eos.commons.utils.GsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import will.test.cluster.MasterInfo;
import will.test.lock.ILockClient;
import will.test.lock.LockServerNaException;

@Slf4j
public class RedisClient implements ILockClient {
    private static final String OK = "OK";
    private static final String MASTER_SUFFIX = ".master";
    private static final String TERM_SUFFIX = ".term";

    private static final Long ONE = 1L;

    private final BullClient redisClient;

    @SneakyThrows
    public RedisClient(String bullName) {
        redisClient = new BullClient(bullName);
    }

    @Override
    public boolean setMasterIfAbsent(String clusterId, MasterInfo master, int expireSeconds) throws LockServerNaException {
        try {
            String res = redisClient.set(clusterId + MASTER_SUFFIX, GsonUtil.toJson(master), "NX", "EX", expireSeconds);
            return OK.equalsIgnoreCase(res);
        } catch (BullException e) {
            throw new LockServerNaException(e);
        }
    }

    @Override
    public boolean refreshMaster(String clusterId, int seconds) throws LockServerNaException {
        try {
            Long res = redisClient.expire(clusterId + MASTER_SUFFIX, seconds);
            return ONE.equals(res);
        } catch (BullException e) {
            throw new LockServerNaException(e);
        }
    }

    @Override
    public MasterInfo getMaster(String clusterId) throws LockServerNaException {
        try {
            String value = redisClient.get(clusterId + MASTER_SUFFIX);
            if (StringUtils.isNotBlank(value)) {
                return GsonUtil.fromJson(value, MasterInfo.class);
            }
            return null;
        } catch (BullException e) {
            throw new LockServerNaException(e);
        }
    }

    @Override
    public long incrMasterTerm(String clusterId) throws LockServerNaException {
        try {
            return redisClient.incr(clusterId + TERM_SUFFIX);
        } catch (BullException e) {
            throw new LockServerNaException(e);
        }
    }
}
