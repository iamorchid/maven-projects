package will.test.bytebuddy.delegation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class DelegationTest {

    public static void main(String[] args) throws Exception {
        Source dynamicObj = new ByteBuddy()
                .subclass(Source.class)
                .method(named("hello")).intercept(MethodDelegation.to(Target.class))
                .make()
                .load(Source.class.getClassLoader())
                .getLoaded()
                .newInstance();

        System.out.println(dynamicObj.hello(30, "Will"));
    }

}

