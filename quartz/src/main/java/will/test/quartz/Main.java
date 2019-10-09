package will.test.quartz;

import org.quartz.*;

import java.util.LinkedList;
import java.util.List;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.JobBuilder.newJob;

class Student {
    private final int age;
    private final String name;

    public Student(int age, String name) {
        this.age = age;
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }
}

public class Main {

    public static void main(String[] args) throws Exception {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        Scheduler sched = schedFact.getScheduler();

        sched.start();

        // define the job and tie it to our HelloJob class
        JobDetail job = newJob(HelloJob.class)
                .withIdentity("myJob-" + 0, "group1")
                .build();

        long now = System.currentTimeMillis();
        long elapsedMSInMin = now % (1000*60);
        int leftMSInMin = (int) (1000 * 60 - elapsedMSInMin);
        System.out.println("left MS: " + leftMSInMin);

        // Trigger the job to run now, and then every 40 seconds
        Trigger trigger = newTrigger()
                .withIdentity("myTrigger-" + 0, "group1")
                .startAt(DateBuilder.futureDate(leftMSInMin, DateBuilder.IntervalUnit.MILLISECOND))
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(60)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        sched.scheduleJob(job, trigger);


        List<Student> students = new LinkedList<>();
        for (int i = 0; i < 1024 * 1024; i++) {
            students.add(new Student(i, "name" + i));
        }


        System.in.read();
        System.exit(-1);
    }
}
