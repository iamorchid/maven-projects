package will.test.node;

import io.netty.util.concurrent.Promise;
import org.mortbay.io.EndPoint;

public interface INode {

    /**
     * Get the address of this node
     * @return
     */
    EndPoint getNodeAddress();

    /**
     * Send a message to this node
     * @param message that would be delivered
     * @return promise which can notify if the message has been sent or not
     */
    Promise<Boolean> sendMessage(Object message);

}
