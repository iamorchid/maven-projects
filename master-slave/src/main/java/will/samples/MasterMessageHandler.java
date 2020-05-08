package will.samples;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import will.test.cluster.IMaster;
import will.test.cluster.NodeInfo;
import will.test.event.master.*;
import will.test.message.IBizMessage;
import will.test.message.IMasterBizMessageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MasterMessageHandler implements IMasterEventHandler, IMasterBizMessageHandler {
    private AtomicReference<IMaster> masterRef = new AtomicReference<>();
    private BlockingQueue<Integer> numbers = new LinkedBlockingQueue<>(128);

    @SneakyThrows
    @Override
    public IBizMessage handle(NodeInfo node, IBizMessage message) {
        IMaster master = masterRef.get();
        if (master == null) {
            return null;
        }

        IBizMessage response = null;
        if (message instanceof NumberMessage) {
            int number = ((NumberMessage) message).getNumber();
            log.info("[M] received number {} from {}", number, node);

            numbers.add(number);
            response = message;

            if (numbers.size() >= 10) {
                List<Integer> allNumbers = new ArrayList<>(10);
                numbers.drainTo(allNumbers, 10);
                master.broadcast(new BatchNumbersMessage(allNumbers)).whenComplete((result, error) -> {
                    log.info("[M] broadcast result: {}, errors: {}, cause: {}", result.success(), result.getErrors(), error);
                });
            }
        } else if (message instanceof StringMessage) {
            log.info("[M] received string [{}] from {}", ((StringMessage) message).getContent(), node);
        }
        return response;
    }

    @SneakyThrows
    @Override
    public void onBecomeMaster(BecomeMasterEvent event) {
        log.info("[M] handled onBecomeMaster event from cluster [{}]", event.getSource().getCluster().getClusterId());
        masterRef.set(event.getSource());
    }

    @Override
    public void onAbdicateEvent(AbdicateEvent event) {
        log.info("[M] handled onAbdicateEvent event");
        masterRef.set(null);
    }

    @Override
    public void onNodeJoin(NodeJoinEvent event) {
        log.info("[M] handled NodeJoinEvent event: {}", event.getNode());
    }

    @Override
    public void onNodeLeave(NodeLeaveEvent event) {
        log.info("[M] handled NodeJoinEvent event: {}", event.getNode());
    }

}
