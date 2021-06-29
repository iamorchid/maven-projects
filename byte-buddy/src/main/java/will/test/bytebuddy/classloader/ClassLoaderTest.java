package will.test.bytebuddy.classloader;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;

import static net.bytebuddy.matcher.ElementMatchers.named;


public class ClassLoaderTest {

    public static void main(String[] args) throws Exception {
        Class clazz1 = new ByteBuddy()
                .subclass(Object.class)
                .name("will.test.bytebuddy.classloader.Student")
                .method(named("toString")).intercept(FixedValue.value("Will Zhang"))
                .make()
                // 使用ClassLoadingStrategy.Default.WRAPPER报错（即同名类已存在）
                .load(ClassLoaderTest.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        System.out.println(clazz1.newInstance());

        Class clazz2 = new ByteBuddy()
                .subclass(Object.class)
                .name("will.test.bytebuddy.classloader.Student")
                .method(named("toString")).intercept(FixedValue.value("Will Zhang"))
                .make()
                // 使用ClassLoadingStrategy.Default.WRAPPER报错（即同名类已存在）
                .load(ClassLoaderTest.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        System.out.println(clazz2.newInstance());

        System.out.println(clazz1 == clazz2);
    }

}
