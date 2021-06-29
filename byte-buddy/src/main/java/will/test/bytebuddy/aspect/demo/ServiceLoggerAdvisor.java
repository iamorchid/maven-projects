package will.test.bytebuddy.aspect.demo;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceLoggerAdvisor {
    /**
     * This can only be public as byte-buddy copies the aspect related code to
     * generated class (namely using in-line way).
     */
    public static AtomicInteger STATS = new AtomicInteger();

    @Advice.OnMethodEnter
    public static void onMethodEnter(@Advice.Origin Method method,
                                     @Advice.This Service target,
                                     @Advice.FieldValue(value = "name", declaringType = Service.class) String service,
                                     @Advice.AllArguments Object[] arguments) {
        STATS.incrementAndGet();

        System.out.println(target);
        System.out.println(service);
        System.out.println("Enter " + method.getName() + " with arguments: " + Arrays.toString(arguments));
    }

    @Advice.OnMethodExit
    public static void onMethodExit(@Advice.Origin Method method,
                                    @Advice.AllArguments Object[] arguments,
                                    @Advice.Return(readOnly = false, typing = Assigner.Typing.STATIC) String ret) {
        STATS.incrementAndGet();

        System.out.println("Exit " + method.getName() + " with arguments: " + Arrays.toString(arguments) + " return: " + ret);

        ret = ret.toUpperCase() + "-" + ret.toLowerCase();
    }
}
