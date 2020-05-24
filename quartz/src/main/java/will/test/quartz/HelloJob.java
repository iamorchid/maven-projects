package will.test.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.io.Serializable;

@Slf4j
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class HelloJob implements Job, Serializable {

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap map = context.getJobDetail().getJobDataMap();

        log.info("[{}][{}]: name={}, count={}",
                context.getJobDetail().getKey(), context.getTrigger().getKey(),
                map.get("name"), map.get("count"));

        context.getJobDetail().getJobDataMap().put("count", map.getIntValue("count") + 1);
    }

}
