package will.test.event.node;

import lombok.Getter;
import will.test.cluster.INode;
import will.test.cluster.MasterInfo;
import will.test.event.BaseEvent;

@Getter
public class LeaveMasterEvent extends BaseEvent<INode> {
    private MasterInfo master;

    public LeaveMasterEvent(INode source, MasterInfo master) {
        super(source);
        this.master = master;
    }
}
