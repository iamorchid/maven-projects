package will.test.event.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.cluster.INode;
import will.test.cluster.MasterInfo;
import will.test.event.BaseEvent;

/**
 * Indicates that the node has successfully joined a master
 */
@Getter
public class JoinMasterEvent extends BaseEvent<INode> {
    private final MasterInfo master;

    public JoinMasterEvent(INode source, MasterInfo master) {
        super(source);
        this.master = master;
    }
}
