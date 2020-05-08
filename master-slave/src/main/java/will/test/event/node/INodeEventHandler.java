package will.test.event.node;

public interface INodeEventHandler {

    INodeEventHandler DUMMY_HANDLER = new INodeEventHandler() {
    };

    /**
     * This event is triggered when the node has started up successfully
     * but before it initially joins the master. And this event would only
     * be triggered once.
     * @param event
     */
    default void onStartupEvent(StartupEvent event) {

    }

    default void onJoinMaster(JoinMasterEvent event) {

    }

    default void onLeaveMaster(LeaveMasterEvent event) {

    }

    default void onClusterStateChange(ClusterStateUpdateEvent event) {

    }
}
