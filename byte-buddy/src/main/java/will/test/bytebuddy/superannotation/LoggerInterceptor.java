package will.test.bytebuddy.superannotation;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.util.List;
import java.util.concurrent.Callable;

public class LoggerInterceptor {

    public static List<String> log(@Argument(0) String info,
                                   @Super MemoryDatabase superProxy,
                                   @SuperCall Callable<List<String>> zuper) throws Exception {
        // 和zuper.call()是等价的，但这里的调用的方法是静态的
        System.out.println(superProxy.load(info));

        System.out.println("Calling database");
        try {
            return zuper.call();
        } finally {
            System.out.println("Returned from database");
        }
    }

}
