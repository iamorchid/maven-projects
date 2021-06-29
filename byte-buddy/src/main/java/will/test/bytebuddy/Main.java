package will.test.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import org.apache.commons.io.FileUtils;

import java.io.File;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class Main {

    public static void main(String[] args) throws Exception {
        Class<? extends Object> clazz = new ByteBuddy()
                .subclass(Object.class)
                .name("will.test.bytebuddy.Book")
                .method(named("toString")).intercept(FixedValue.value("Understand Linux Kernel"))
                .make()
                .load(Main.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        System.out.println(clazz);
        System.out.println(Main.class.getClassLoader().loadClass("will.test.bytebuddy.Book"));
        System.out.println(Main.class.getClassLoader().loadClass("will.test.bytebuddy.Book") == clazz);
    }

}
