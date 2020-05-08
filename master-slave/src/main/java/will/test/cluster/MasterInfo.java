package will.test.cluster;

import com.envision.eos.commons.transport.EndPoint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class MasterInfo implements Serializable {
    private final String masterId;
    private final EndPoint endpoint;

    public String getGlobalId(String clusterId) {
        return "[" + clusterId + "][" + endpoint + "][" + masterId + "]";
    }

    @Override
    public String toString() {
        return "[" + endpoint + "][" + masterId + "]";
    }
}
