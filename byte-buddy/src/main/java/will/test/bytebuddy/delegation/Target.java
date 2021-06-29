package will.test.bytebuddy.delegation;

import net.bytebuddy.implementation.bind.annotation.Super;

public class Target {

//    public static String hello(int age, String name, @Super Source source) {
//        System.out.println(source.hello(age - 3, name));
//        return "hello, " + name + "!";
//    }

    public static String hello(int age, String name) {
        return "hello, " + name + ", you are " + age + " years old!";
    }

    public static String hello(String name) {
        return "hello, " + name + "!";
    }
}
