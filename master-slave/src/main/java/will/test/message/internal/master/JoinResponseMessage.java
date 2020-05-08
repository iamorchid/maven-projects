package will.test.message.internal.master;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JoinResponseMessage implements IMasterMessage {
    public final static int CODE_SUCCESS = 0;
    public final static int CODE_CLUSTER_UNMATCHED = 1;

    private int code;
    private String masterId;
    private String requestId;
    private String error;
}
