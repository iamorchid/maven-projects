package will.test.message.internal.master;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.message.IBizMessage;

@AllArgsConstructor
@Getter
public class HeartbeatResponseMessage implements IMasterMessage {
    private String masterId;
    private IBizMessage payload;
}
