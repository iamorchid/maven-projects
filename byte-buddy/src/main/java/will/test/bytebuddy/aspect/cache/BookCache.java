package will.test.bytebuddy.aspect.cache;

import net.bytebuddy.asm.Advice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.STATIC;

public class BookCache {
    /**
     * This can only be public as byte-buddy copies the aspect related code to
     * generated class (namely using in-line way).
     */
    public final static Map<String, Book> CACHE = new ConcurrentHashMap<>();

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static Book beforeGetBook(@Advice.Argument(0) String bookName) {
        return CACHE.get(bookName);
    }

    @Advice.OnMethodExit
    public static void afterGetBook(
            @Advice.Argument(0) String bookName,
            @Advice.Return(readOnly = false, typing = STATIC) Book returned,
            @Advice.Enter Book fromEnterMethod) {
        if (fromEnterMethod != null) {
            returned = fromEnterMethod;
        } else if (returned != null) {
            CACHE.put(bookName, returned);
        }
    }
}
