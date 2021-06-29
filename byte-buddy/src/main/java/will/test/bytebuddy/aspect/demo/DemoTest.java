package will.test.bytebuddy.aspect.demo;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class DemoTest {

    public static void main(String[] args) throws Exception {
        DynamicType.Unloaded<Service> unloaded = new ByteBuddy()
                .subclass(Service.class)
                .method(ElementMatchers.isAnnotatedWith(Log.class))
                .intercept(Advice.to(ServiceLoggerAdvisor.class))
                .make();

        FileUtils.writeByteArrayToFile(new File("C:\\Users\\jian.zhang4\\tmep\\BuddyClass.class"),
                unloaded.getBytes());

        Service service = unloaded
                .load(DemoTest.class.getClassLoader())
                .getLoaded()
                .getConstructor(String.class)
                .newInstance("SLS");

        int mainBar = service.bar(123);
        System.out.println("mainBar = " + mainBar);

        String mainFoo = service.foo("hello");
        System.out.println("mainFoo = " + mainFoo);
    }

}
