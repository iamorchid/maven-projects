package will.test.bytebuddy.delegation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.commons.io.FileUtils;

import java.io.File;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class DelegationTest {

    public static void main(String[] args) throws Exception {
        DynamicType.Unloaded<Source> unloaded = new ByteBuddy()
                .subclass(Source.class)
                .method(named("hello")).intercept(MethodDelegation.to(Target.class))
                .make();

        FileUtils.writeByteArrayToFile(new File("C:\\Users\\jian.zhang4\\tmep\\BuddyClass.class"),
                unloaded.getBytes());

        Source dynamicObj = unloaded
                .load(Source.class.getClassLoader())
                .getLoaded()
                .newInstance();

        System.out.println(dynamicObj.hello(30, "Will"));
    }

}
