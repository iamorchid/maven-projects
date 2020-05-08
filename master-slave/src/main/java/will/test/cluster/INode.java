package will.test.cluster;

import will.test.message.IBizMessage;

import java.util.concurrent.CompletableFuture;

public interface INode {
    /**
     * Return the current state of the cluster
     * @return
     */
    ClusterState getCluster();

    /**
     * Get the information of the node
     * @return
     */
    NodeInfo getSelfInfo();

    /**
     * Send a message to the current master
     * @return
     */
    CompletableFuture<Boolean> send(IBizMessage message);

    /**
     * Send a message to a specific peer node
     * @param peerNode
     * @param message
     * @return
     */
    CompletableFuture<Boolean> send(NodeInfo peerNode, IBizMessage message);
}
