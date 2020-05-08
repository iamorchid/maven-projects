package will.test.cluster.internal.node;

import com.envision.eos.commons.transport.EndPoint;
import lombok.Getter;
import will.test.cluster.MasterInfo;

import java.util.concurrent.ScheduledFuture;

/**
 * This is internal master info managed in a node
 */
@Getter
public class InternalMasterInfo {
    private final MasterInfo master;
    private final ScheduledFuture heartbeatSchedule;

    private volatile long heartbeatRspLast;

    public InternalMasterInfo(MasterInfo master, ScheduledFuture heartbeatSchedule) {
        this.master = master;
        this.heartbeatSchedule = heartbeatSchedule;
        this.heartbeatRspLast = System.nanoTime();
    }

    public String getExplainableId(String clusterId) {
        return master.getGlobalId(clusterId);
    }

    public String getId() {
        return master.getMasterId();
    }

    public EndPoint getEndpoint() {
        return master.getEndpoint();
    }

    public void refreshHeartbeatResponse() {
        heartbeatRspLast = System.nanoTime();
    }
}
