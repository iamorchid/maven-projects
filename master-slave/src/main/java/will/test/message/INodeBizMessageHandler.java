package will.test.message;

/**
 * business related message handler invoked by master
 */
@FunctionalInterface
public interface INodeBizMessageHandler {

    INodeBizMessageHandler DUMMY = (message) -> null;

    /**
     * Handle a message delivered to local node. Note that this message
     * could be from the master or a peer node.
     * @param message
     * @return
     */
    IBizMessage handle(IBizMessage message);
}
