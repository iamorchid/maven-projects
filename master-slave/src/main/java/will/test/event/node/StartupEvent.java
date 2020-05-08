package will.test.event.node;

import lombok.Getter;
import will.test.cluster.INode;
import will.test.event.BaseEvent;

/**
 * Indicates the local node has started successfully and this event would
 * only be triggered once through-out the lifecycle of the node.
 */
@Getter
public class StartupEvent extends BaseEvent<INode> {

    public StartupEvent(INode source) {
        super(source);
    }

}
