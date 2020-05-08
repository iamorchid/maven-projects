package will.test.cluster;

import will.test.message.IBizMessage;

import java.util.concurrent.CompletableFuture;

public interface IMaster {
    /**
     * Return the current state of the cluster
     * @return
     */
    ClusterState getCluster() throws NoLongerMasterException;

    /**
     * Get the information of the master
     * @return
     */
    MasterInfo getSelfInfo() throws NoLongerMasterException;

    /**
     * broadcast the message to all nodes in cluster
     * @param message
     * @return
     */
    CompletableFuture<BroadcastResult> broadcast(IBizMessage message) throws NoLongerMasterException;


    /**
     * Send a message to a specific node
     * @param node
     * @param message
     * @return
     */
    CompletableFuture<Boolean> send(NodeInfo node, IBizMessage message) throws NoLongerMasterException;

}
