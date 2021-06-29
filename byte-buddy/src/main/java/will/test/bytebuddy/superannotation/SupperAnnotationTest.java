package will.test.bytebuddy.superannotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.commons.io.FileUtils;

import java.io.File;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class SupperAnnotationTest {

    public static void main(String[] args) throws Exception {
        DynamicType.Unloaded<MemoryDatabase> unloaded = new ByteBuddy()
                .subclass(MemoryDatabase.class)
                .method(named("load")).intercept(MethodDelegation.to(LoggerInterceptor.class))
                .make();


        FileUtils.writeByteArrayToFile(new File("C:\\Users\\jian.zhang4\\tmep\\BuddyClass.class"),
                unloaded.getBytes());

        MemoryDatabase loggingDatabase = unloaded
                .load(SupperAnnotationTest.class.getClassLoader())
                .getLoaded()
                .newInstance();

        System.out.println("loggingDatabase: " + loggingDatabase);
        System.out.println(loggingDatabase.load("test"));
    }

}
