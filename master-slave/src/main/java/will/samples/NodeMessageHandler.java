package will.samples;

import lombok.extern.slf4j.Slf4j;
import will.test.cluster.INode;
import will.test.event.node.*;
import will.test.message.IBizMessage;
import will.test.message.INodeBizMessageHandler;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NodeMessageHandler implements INodeEventHandler, INodeBizMessageHandler {
    private AtomicReference<INode> node = new AtomicReference<>();
    private AtomicReference<ScheduledFuture> scheduledTask = new AtomicReference<>();

    @Override
    public IBizMessage handle(IBizMessage message) {
        if (message instanceof NumberMessage) {
            log.info("[N]    received one number: {}", ((NumberMessage) message).getNumber());
        } else if (message instanceof BatchNumbersMessage) {
            log.info("[N] received batch numbers: {}", ((BatchNumbersMessage) message).getNumbers());
        } else if (message instanceof StringMessage) {
            log.info("[N]        received string: {}", ((StringMessage) message).getContent());
        }
        return null;
    }

    @Override
    public void onStartupEvent(StartupEvent event) {
        log.info("[N] handled onStartupEvent from {}", event.getSource().getSelfInfo());
        node.set(event.getSource());
    }

    @Override
    public void onJoinMaster(JoinMasterEvent event) {
        log.info("[N] handled onJoinMaster");
        scheduledTask.set(Helper.SCHEDULED_SERVICE.scheduleAtFixedRate(() -> {
            node.get().send(new NumberMessage(new Random().nextInt(10)));
        }, 1, 2, TimeUnit.SECONDS));
    }

    @Override
    public void onLeaveMaster(LeaveMasterEvent event) {
        log.info("[N] handled onLeaveMaster");
        scheduledTask.get().cancel(false);
    }

    @Override
    public void onClusterStateChange(ClusterStateUpdateEvent event) {
        log.info("[N] handled onClusterStateChange: {}", event.getNewState().getNodes());
    }

}
