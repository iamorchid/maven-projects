package will.test.event.node;

import lombok.Getter;
import will.test.cluster.ClusterState;
import will.test.cluster.INode;
import will.test.event.BaseEvent;

@Getter
public class ClusterStateUpdateEvent extends BaseEvent<INode> {
    private ClusterState newState;

    public ClusterStateUpdateEvent(INode source, ClusterState newState) {
        super(source);
        this.newState = newState;
    }
}
