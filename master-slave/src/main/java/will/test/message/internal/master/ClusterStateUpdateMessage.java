package will.test.message.internal.master;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.cluster.ClusterState;

@AllArgsConstructor
@Getter
public class ClusterStateUpdateMessage implements IMasterMessage {
    private ClusterState current;

    @Override
    public String getMasterId() {
        return current.getMaster().getMasterId();
    }
}
