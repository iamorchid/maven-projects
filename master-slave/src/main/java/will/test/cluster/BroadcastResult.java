package will.test.cluster;

import lombok.Getter;

import java.util.Map;

@Getter
public class BroadcastResult {
    private final int nodeCount;
    private final int successCount;
    private final int failCount;
    private final Map<NodeInfo, Throwable> errors;

    public BroadcastResult(int nodeCount, Map<NodeInfo, Throwable> errors) {
        this.nodeCount = nodeCount;
        this.successCount = nodeCount - errors.size();
        this.failCount = errors.size();
        this.errors = errors;
    }

    public boolean success() {
        return nodeCount == successCount;
    }
}
