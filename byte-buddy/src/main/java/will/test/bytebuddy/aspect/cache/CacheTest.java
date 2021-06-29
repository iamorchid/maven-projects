package will.test.bytebuddy.aspect.cache;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.DynamicType;
import org.apache.commons.io.FileUtils;

import java.io.File;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class CacheTest {

    public static void main(String[] args) throws Exception {
        DynamicType.Unloaded<BookStore> unloaded = new ByteBuddy()
                .subclass(BookStore.class)
                .method(named("get").and(takesArguments(String.class)))
                .intercept(Advice.to(BookCache.class))
                .make();

        FileUtils.writeByteArrayToFile(new File("C:\\Users\\jian.zhang4\\tmep\\BuddyClass.class"),
                unloaded.getBytes());

        BookStore bookStore = unloaded
                .load(CacheTest.class.getClassLoader())
                .getLoaded()
                .newInstance();

        System.out.println(bookStore.get("ULK") == bookStore.get("ULK"));
    }

}
