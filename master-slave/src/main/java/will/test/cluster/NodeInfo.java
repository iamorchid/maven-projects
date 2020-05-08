package will.test.cluster;

import com.envision.eos.commons.transport.EndPoint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class NodeInfo implements Serializable {
    private final String hostName;
    private final EndPoint endpoint;

    @Override
    public String toString() {
        return "[" + hostName + "][" + endpoint + "]";
    }
}
