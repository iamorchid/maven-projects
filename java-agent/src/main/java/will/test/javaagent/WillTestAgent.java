package will.test.javaagent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class WillTestAgent {

    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("loaded will's java agent");
        new AgentBuilder.Default()
                .type(isAnnotatedWith(named("will.test.bytebuddy.agent.ToJsonString")))
                .transform((builder, typeDescription, classLoader, module) -> {
//                    System.out.println("transformed " + typeDescription.getName());
                    return builder
                            .method(named("toString")).intercept(MethodDelegation.to(ToJsonStringDelegator.class));
                })
                .type(ElementMatchers.any())
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .include(DurationAdvisor.class.getClassLoader())
                        .advice(
                                isAnnotatedWith(named("will.test.bytebuddy.agent.StatDuration")),
                                DurationAdvisor.class.getName())
                )
                .installOn(instrumentation);
    }
}
