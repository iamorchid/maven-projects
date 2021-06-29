package will.test.javaagent;

import com.google.gson.Gson;
import net.bytebuddy.implementation.bind.annotation.This;

public class ToJsonStringDelegator {
    public final static Gson GSON = new Gson();

    public static String toJson(@This Object target) {
        return GSON.toJson(target);
    }
}
