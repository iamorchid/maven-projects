package will.test.cluster.internal.master;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.cluster.NodeInfo;

import java.util.concurrent.ScheduledFuture;

/**
 * This is internal node info managed in the master.
 */
@AllArgsConstructor
@Getter
public class InternalNodeInfo {
    /**
     * Node information
     */
    private final NodeInfo node;

    /**
     * This is used to check if the node connection to master has timed out
     */
    private final ScheduledFuture timeoutTask;

    /**
     * This would be changed and accessed in multiple threads
     */
    private volatile long heartbeatLast;

    public void refreshHeartbeat() {
        this.heartbeatLast = System.nanoTime();
    }
}
