package will.test.event.master;

public interface IMasterEventHandler {

    public final static IMasterEventHandler DUMMY_HANDLER = new IMasterEventHandler() {
    };

    default void onBecomeMaster(BecomeMasterEvent event) {

    }

    default void onAbdicateEvent(AbdicateEvent event) {

    }

    default void onNodeJoin(NodeJoinEvent event) {

    }

    default void onNodeLeave(NodeLeaveEvent event) {

    }

}
