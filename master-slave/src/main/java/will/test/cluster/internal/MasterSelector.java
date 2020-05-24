package will.test.cluster.internal;

import com.envision.eos.commons.transport.EndPoint;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import will.test.Config;
import will.test.cluster.MasterInfo;
import will.test.cluster.NoLongerMasterException;
import will.test.cluster.internal.master.Master;
import will.test.common.Utils;
import will.test.lock.LockServerNaException;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MasterSelector {
    private final Config config;
    private final EndPoint localMasterAddress;
    private volatile Master master;

    @SneakyThrows
    public MasterSelector(Config config) {
        this.config = config;
        this.localMasterAddress = new EndPoint(config.getPublicIp(), config.getMasterPort());
    }

    public synchronized CompletableFuture<MasterInfo> select(boolean isAppStartup) {
        CompletableFuture<MasterInfo> result = null;

        // At least, we can try 3 times by default
        long maxRetries = isAppStartup ? 3 : Long.MAX_VALUE;

        do {
            try {
                result = doSelect();
            } catch (LockServerNaException lockError) {
                log.error("redis not available for the moment", lockError);
                Utils.safeSleep(3000);
            } catch (NoLongerMasterException error) {
                // clean the destroyed master and this should not waste the retry
                stopMaster();
                maxRetries += 1;
            } catch (Throwable e) {
                log.error("[BUG] hit un-expected error", e);
            }
            maxRetries -= 1;
        } while (maxRetries > 0 && result == null);

        return result;
    }

    private CompletableFuture<MasterInfo> doSelect() throws LockServerNaException, NoLongerMasterException {
        final MasterInfo persistedMaster = config.getLockImpl().getMaster(config.getClusterId());

        if (persistedMaster != null) {
            log.debug("found persisted master {}", persistedMaster.getGlobalId(config.getClusterId()));

            // We need to destroy local started master if the mastership has transferred
            if (master != null) {
                if (persistedMaster.equals(master.getSelfInfo())) {
                    log.debug("local host has already been the master for [{}]", config.getClusterId());
                    return CompletableFuture.completedFuture(master.getSelfInfo());
                }

                log.warn("local master {} has been replaced by {}", master.getSelfInfo(), persistedMaster);
                stopMaster();
            }

            return CompletableFuture.completedFuture(persistedMaster);
        }

        log.debug("found no persistent master, try becoming the master of cluster [{}]", config.getClusterId());

        // Since we need some time to start master node, we need somewhat longer expire time
        final long masterTerm = config.getLockImpl().incrMasterTerm(config.getClusterId());
        final MasterInfo masterInfo = new MasterInfo(String.valueOf(masterTerm), localMasterAddress);
        final int initialExpire = config.getLockExpireAfter() * 2;
        if (!config.getLockImpl().setMasterIfAbsent(config.getClusterId(), masterInfo, initialExpire)) {
            // some one else has just become the master, need to go through another round
            log.warn("need to re-select since some one's has just become the master of [{}]", config.getClusterId());
            return doSelect();
        }

        log.debug("local host has become the master of cluster [{}]", config.getClusterId());

        if (master != null) {
            // This means the master has not refresh its persistent time within the expire time.
            // Definitely there could be potential bug somewhere.
            log.error("[BUG] destroy previously started but now dead master {} of cluster {}",
                    master.getSelfInfo(), config.getClusterId());
            master.destroy();
        }

        master = new Master(config, masterInfo);
        return master.start();
    }

    public synchronized void stopMaster() {
        if (master != null) {
            master.destroy();
            master = null;
        }
    }

}
