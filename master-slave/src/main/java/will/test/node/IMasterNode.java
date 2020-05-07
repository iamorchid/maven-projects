package will.test.node;

import java.util.List;

public interface IMasterNode extends INode {

    List<ISlaveNode> getSlaves();

}
