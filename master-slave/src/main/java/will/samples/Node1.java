package will.samples;

import lombok.extern.slf4j.Slf4j;
import will.test.Config;
import will.test.cluster.Cluster;
import will.test.lock.redis.RedisClient;

@Slf4j
public class Node1 {

    public static void main(String[] args) throws Exception {
        MasterMessageHandler masterHandler = new MasterMessageHandler();
        NodeMessageHandler nodeHandler = new NodeMessageHandler();

        Config config = Config.builder()
                .clusterId(Helper.TEST_MASTER_KEY)
                .lockImpl(new RedisClient("Bull.eos-cloud"))
                .masterPort(7780)
                .nodePort(7781)
                .masterEventHandler(masterHandler)
                .masterMsgHandler(masterHandler)
                .nodeEventHandler(nodeHandler)
                .nodeMsgHandler(nodeHandler)
                .heartbeatReqPayloadSupplier(() -> new StringMessage("[N] node#1"))
                .heartbeatRspPayloadSupplier(n -> new StringMessage("[M] " + n.getEndpoint()))
                .hostName("node#1")
                .build();

        Cluster cluster = new Cluster(config);

        log.info("start node now");
        cluster.start().whenComplete((done, error) -> {
            log.info("start completed with done: {}, error: {}", done, error);
        });

        System.in.read();
    }

}
