package will.test.event.master;

import lombok.Getter;
import will.test.cluster.IMaster;
import will.test.event.BaseEvent;

/**
 * Indicates that the master has abdicated and some other node has taken
 * over the mastership of the cluster. And the user should make use of
 * the master any more after this event.
 *
 * @author jian.zhang4
 */
@Getter
public class AbdicateEvent extends BaseEvent<IMaster> {

    public AbdicateEvent(IMaster source) {
        super(source);
    }

}
