package will.test.quartz;

import org.quartz.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Main {

    public static void main(String[] args) throws Exception {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        Scheduler sched = schedFact.getScheduler();

        sched.start();

        // define the job and tie it to our HelloJob class
        JobDetail job = newJob(HelloJob.class)
                .usingJobData("name", "Will Zhang")
                .withIdentity("myJob-" + 0, "group1")
                .build();

        long now = System.currentTimeMillis();
        long elapsedMillis = now % (1000 * 60);
        int remainingMillis = (int) (1000 * 60 - elapsedMillis);
        System.out.println("remaining millis: " + remainingMillis);

        // Trigger the job to run now, and then every 40 seconds
        Trigger trigger = newTrigger()
                .withIdentity("myTrigger-" + 0, "group1")
                .startAt(DateBuilder.futureDate(remainingMillis, DateBuilder.IntervalUnit.MILLISECOND))
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(60)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        sched.scheduleJob(job, trigger);

        // Trigger the job to run now, and then every 40 seconds
        trigger = newTrigger()
                .withIdentity("myTrigger-" + 1, "group1")
                .startAt(DateBuilder.futureDate(remainingMillis + 5000, DateBuilder.IntervalUnit.MILLISECOND))
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(60)
                        .repeatForever())
                .forJob(job.getKey())
                .build();

        // Tell quartz to schedule the job using our trigger
        sched.scheduleJob(trigger);

        System.in.read();
    }
}
