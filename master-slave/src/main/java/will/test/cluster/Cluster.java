package will.test.cluster;

import will.test.Config;
import will.test.cluster.internal.node.Node;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class Cluster {
    private final Config config;
    private final AtomicReference<Node> nodeRef = new AtomicReference<>();

    public Cluster(Config config) {
        this.config = config;
    }

    public CompletableFuture<Boolean> start() {
        nodeRef.set(new Node(config));
        return nodeRef.get().start();
    }

    public void destroy() {
        Node node = nodeRef.getAndSet(null);
        if (node != null) {
            node.destroy();
        }
    }
}
