package will.test.message;

import will.test.cluster.NodeInfo;

/**
 * business related message handler invoked by master
 */
@FunctionalInterface
public interface IMasterBizMessageHandler {

    IMasterBizMessageHandler DUMMY = (node, message) -> null;

    /**
     * Handle a message from a specific node and a response message can be optionally returned.
     * @param node
     * @param message
     * @return
     */
    IBizMessage handle(NodeInfo node, IBizMessage message);
}
