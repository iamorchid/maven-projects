package will.test.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.Serializable;
import java.util.Date;

public class HelloJob implements Job, Serializable {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("-------" + context.getJobDetail().getKey() + "------");
        System.out.println(new Date());
        System.out.println(context);
    }

}
