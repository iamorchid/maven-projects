package will.test.cluster;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@Getter
public class ClusterState implements Serializable {
    private String clusterId;
    private MasterInfo master;
    private List<NodeInfo> nodes;
}
