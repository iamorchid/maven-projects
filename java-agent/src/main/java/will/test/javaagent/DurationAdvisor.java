package will.test.javaagent;

import net.bytebuddy.asm.Advice;

/**
 * @author jian.zhang4
 */
public class DurationAdvisor {

    @Advice.OnMethodEnter
    public static long enter() {
        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit
    public static void exit(
            @Advice.Origin(value = "#m") String method,
            @Advice.This Object target,
            @Advice.Enter long start) {
        long takes = System.currentTimeMillis() - start;
        System.out.println(target.getClass().getSimpleName() + "/" + method + " takes " + takes + "ms");
    }

}
