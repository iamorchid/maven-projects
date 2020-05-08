package will.test.message.internal.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.cluster.NodeInfo;
import will.test.message.internal.IInternalMessage;

@AllArgsConstructor
@Getter
public class JoinRequestMessage implements IInternalMessage {
    private String requestId;
    private String clusterId;
    private NodeInfo node;
}
