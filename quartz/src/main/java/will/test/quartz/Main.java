package will.test.quartz;

import org.quartz.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Note that we need to manually create the quartz tables needed.
 * See org\quartz\impl\jdbcjobstore\tables_mysql.sql in quartz jar file.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        Scheduler sched = schedFact.getScheduler();
        sched.start();

        sched.pauseJob("");

        JobKey jobKey = JobKey.jobKey("job#0", "group#1");

        if (!sched.checkExists(jobKey)) {
            System.out.println("add new job " + jobKey);

            // define the job and tie it to our HelloJob class
            JobDetail job = newJob(HelloJob.class)
                    .usingJobData("name", "Will Zhang")
                    .usingJobData("count", 1)
                    .withIdentity(jobKey)
                    .build();

            long now = System.currentTimeMillis();
            long elapsedMillis = now % (1000 * 60);
            int remainingMillis = (int) (1000 * 60 - elapsedMillis);
            System.out.println("remaining millis: " + remainingMillis);

            // Trigger the job to run now, and then every 40 seconds
            Trigger trigger = newTrigger()
                    .withIdentity("trigger#" + 0, "group#1")
                    .startAt(DateBuilder.futureDate(remainingMillis, DateBuilder.IntervalUnit.MILLISECOND))
                    .withSchedule(simpleSchedule()
                            .withIntervalInSeconds(60)
                            .repeatForever())
                    .build();

            // Tell quartz to schedule the job using our trigger
            sched.scheduleJob(job, trigger);

            // Trigger the job to run now, and then every 40 seconds
            trigger = newTrigger()
                    .withIdentity("trigger#" + 1, "group#1")
                    .startAt(DateBuilder.futureDate(remainingMillis + 5000, DateBuilder.IntervalUnit.MILLISECOND))
                    .withSchedule(simpleSchedule()
                            .withIntervalInSeconds(60)
                            .repeatForever())
                    .forJob(job.getKey())
                    .build();

            // Tell quartz to schedule the job using our trigger
            sched.scheduleJob(trigger);
        }

        System.in.read();
    }
}
