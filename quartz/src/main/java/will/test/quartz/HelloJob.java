package will.test.quartz;

import lombok.Getter;
import lombok.Setter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.Serializable;
import java.util.Date;

public class HelloJob implements Job, Serializable {

    @Getter @Setter
    private String name;

    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("date: " + new Date());
        System.out.println("jobKey: " + context.getJobDetail().getKey());
        System.out.println("trigger: " + context.getTrigger().getKey());
    }

}
