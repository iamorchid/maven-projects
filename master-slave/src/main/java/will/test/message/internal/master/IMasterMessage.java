package will.test.message.internal.master;

import will.test.message.internal.IInternalMessage;

/**
 * Interface for message from the master
 */
public interface IMasterMessage extends IInternalMessage {

    String getMasterId();

}
