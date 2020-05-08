package will.test.event.master;

import lombok.Getter;
import will.test.cluster.IMaster;
import will.test.event.BaseEvent;

/**
 * Indicates that the local host has become the master of a cluster. Note that
 * a master could abdicate later and re-become master in the future. So this
 * event and {@link AbdicateEvent} could be triggered multiple times.
 *
 * @author jian.zhang4
 */
@Getter
public class BecomeMasterEvent extends BaseEvent<IMaster> {

    public BecomeMasterEvent(IMaster source) {
        super(source);
    }

}
