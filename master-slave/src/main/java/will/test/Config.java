package will.test;

import com.envision.eos.commons.transport.HostPublicIpResolver;
import com.envision.eos.commons.utils.HostUtil;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import will.test.cluster.NodeInfo;
import will.test.event.master.IMasterEventHandler;
import will.test.event.node.INodeEventHandler;
import will.test.lock.ILockClient;
import will.test.message.IBizMessage;
import will.test.message.IMasterBizMessageHandler;
import will.test.message.INodeBizMessageHandler;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Builder
@Getter
public class Config {

    /**
     * This is used to identify the cluster
     */
    private String clusterId;

    private ILockClient lockImpl;

    /**
     * The time duration after which the cluster id lock expires if there is no refresh for it.
     */
    @Builder.Default
    private int lockExpireAfter = 3;

    @Builder.Default
    private String hostName = HostUtil.getLocalhost();

    @Builder.Default
    private String publicIp = HostPublicIpResolver.getPublicIp();

    @Builder.Default
    private int masterPort = 7800;

    @Builder.Default
    private int nodePort = 7900;

    @Builder.Default
    private IMasterEventHandler masterEventHandler = null;

    @Builder.Default
    private INodeEventHandler nodeEventHandler = null;

    @Builder.Default
    private IMasterBizMessageHandler masterMsgHandler = null;

    @Builder.Default
    private INodeBizMessageHandler nodeMsgHandler = null;

    @Builder.Default
    private int heatbeatPeriod = 1; // seconds

    @Builder.Default
    private int heartbeatTimeout = 3; // seconds

    @Builder.Default
    @Getter(AccessLevel.PRIVATE)
    private Supplier<IBizMessage> heartbeatReqPayloadSupplier = null;

    /**
     * Allow the master to prepare heartbeat response message for specific node
     */
    @Builder.Default
    @Getter(AccessLevel.PRIVATE)
    private Function<NodeInfo, IBizMessage> heartbeatRspPayloadSupplier = null;

    /**
     * How many threads are used for the cluster management
     */
    @Builder.Default
    private int nThreads = 2;

    public void validate() {
        if (StringUtils.isBlank(clusterId)) {
            throw new IllegalStateException("clusterId not set");
        }

        if (lockImpl == null) {
            throw new IllegalStateException("lockImpl not set");
        }
    }

    public String getHostName() {
        return StringUtils.isNotBlank(hostName) ? hostName : HostUtil.getLocalhost();
    }

    public String getPublicIp() {
        return StringUtils.isNotBlank(publicIp) ? publicIp : HostPublicIpResolver.getPublicIp();
    }

    public IMasterEventHandler getMasterEventHandler() {
        return masterEventHandler != null ? masterEventHandler : IMasterEventHandler.DUMMY_HANDLER;
    }

    public INodeEventHandler getNodeEventHandler() {
        return nodeEventHandler != null ? nodeEventHandler : INodeEventHandler.DUMMY_HANDLER;
    }

    public IMasterBizMessageHandler getMasterMsgHandler() {
        return masterMsgHandler != null ? masterMsgHandler : IMasterBizMessageHandler.DUMMY;
    }

    public INodeBizMessageHandler getNodeMsgHandler() {
        return nodeMsgHandler != null ? nodeMsgHandler : INodeBizMessageHandler.DUMMY;
    }

    public IBizMessage getHeartbeatReqPayload() {
        return heartbeatReqPayloadSupplier != null ? heartbeatReqPayloadSupplier.get() : null;
    }

    public IBizMessage getHeartbeatRspPayload(final NodeInfo node) {
        return heartbeatRspPayloadSupplier != null ? heartbeatRspPayloadSupplier.apply(node) : null;
    }

    public long getHeartbeatTimeoutNs() {
        return TimeUnit.SECONDS.toNanos(heartbeatTimeout);
    }
}
