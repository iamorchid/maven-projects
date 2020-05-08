package will.test.message.internal.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.message.IBizMessage;
import will.test.message.internal.IInternalMessage;

@AllArgsConstructor
@Getter
public class HeartbeatRequestMessage implements IInternalMessage {
    private IBizMessage payload;
}
