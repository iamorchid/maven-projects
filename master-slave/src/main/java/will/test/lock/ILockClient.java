package will.test.lock;

import will.test.cluster.MasterInfo;

public interface ILockClient {

    /**
     * Set the master info of the cluster if it's absent atomically.
     * @param clusterId the identifier of a cluster
     * @param master master information
     * @param expireSeconds the initial expire time of the master information
     * @return
     * @throws LockServerNaException
     */
    boolean setMasterIfAbsent(String clusterId, MasterInfo master, int expireSeconds) throws LockServerNaException;

    /**
     * Refresh the expire time of the master info for the cluster.
     * @param clusterId the identifier of a cluster
     * @param seconds the new expire time of the master information
     * @return true if new expire time is set. Otherwise, it means that the master information doesn't exist any more.
     * @throws LockServerNaException
     */
    boolean refreshMaster(String clusterId, int seconds) throws LockServerNaException;

    /**
     * Get the master information of the cluster
     * @param clusterId the identifier of a cluster
     * @return current master information
     * @throws LockServerNaException
     */
    MasterInfo getMaster(String clusterId) throws LockServerNaException;

    /**
     * Increase and return the new master term
     * @param clusterId
     * @return
     * @throws LockServerNaException
     */
    long incrMasterTerm(String clusterId) throws LockServerNaException;
}
