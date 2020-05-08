package will.samples;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Helper {
    public final static String TEST_MASTER_KEY = "test.master.key";

    public final static ScheduledExecutorService SCHEDULED_SERVICE = new ScheduledThreadPoolExecutor(
            Math.min(Runtime.getRuntime().availableProcessors(), 1),
            new ThreadFactoryBuilder()
                    .setNameFormat("schedule-thread-%d")
                    .build());

}
