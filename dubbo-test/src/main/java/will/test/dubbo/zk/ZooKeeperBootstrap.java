package will.test.dubbo.zk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZooKeeperBootstrap {

    public static void main(String[] args) throws Exception {
        new EmbeddedZooKeeper(2181, false).start();
        log.info("embedded zoo keeper started");
    }

}
