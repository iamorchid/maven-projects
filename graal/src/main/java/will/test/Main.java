package will.test;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

public class Main {
    private static final String ID = "js";
    private static final String JS_SYNTAX_EXTENSIONS_OPTION = "js.syntax-extensions";
    private static final String JS_LOAD_OPTION = "js.load";
    private static final String JS_PRINT_OPTION = "js.print";
    private static final String JS_STRICT = "js.strict";

    public static void main(String[] args) throws Exception {
        Context.Builder contextBuilder = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowHostClassLoading(true)
                .allowCreateThread(false)
                .allowIO(false)
                .option(JS_SYNTAX_EXTENSIONS_OPTION, "false")
                .option(JS_LOAD_OPTION, "false")
                .option(JS_PRINT_OPTION, "false");
//              .option(JS_STRICT, "true");
        Engine engine = Engine.newBuilder()
                .allowExperimentalOptions(true)
                .build();
        GraalJSScriptEngine underlying = GraalJSScriptEngine.create(engine, contextBuilder);

        underlying.eval("function combine(left, right) {\n" +
                "    var arr = [];\n" +
                "    arr.push(left);\n" +
                "    arr.push(right);\n" +
                "    return arr;\n" +
                "}\n" +
                "\n" +
                "function mangled_combine(left, right) {\n" +
                "    var result = combine(left, right);\n" +
                "    return Java.to(result, 'byte[]');\n" +
                "}\n" +
                "\n" +
                "function dict() {\n" +
                "    var map = {};\n" +
                "    map[\"name\"] = \"will\";\n" +
                "    map[\"age\"] = 30;\n" +
                "    return map;\n" +
                "}");
        Object ret = underlying.invokeFunction( "mangled_combine", 20, 30);
        Object dict = underlying.invokeFunction( "dict");
        System.out.println(ret);
    }

}
