package will.test.bytebuddy.attribute;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class AttributeTest {

    public static void main(String[] args) throws Exception {
        Class<? extends  Worker> dynamicWorker = new ByteBuddy()
                .subclass(Worker.class)
                .method(not(isDeclaredBy(Object.class)))
                // 拦截委托给属性字段interceptor
                .intercept(MethodDelegation.toField("interceptor"))
                // 定义一个属性字段
                .defineField("interceptor", Interceptor.class, Visibility.PRIVATE)
                // 实现 InterceptionAccessor 接口
                .implement(InterceptorAccessor.class).intercept(FieldAccessor.ofBeanProperty())
                .make()
                .load(AttributeTest.class.getClassLoader())
                .getLoaded();

        WorkerFactory workerFactory = new ByteBuddy()
                .subclass(WorkerFactory.class)
                .method(not(isDeclaredBy(Object.class)))
                .intercept(MethodDelegation.toConstructor(dynamicWorker))
                .make()
                .load(dynamicWorker.getClassLoader())
                .getLoaded()
                .newInstance();

        Worker worker = (Worker) workerFactory.makeInstance();
        ((InterceptorAccessor) worker).setInterceptor(new Interceptor() {
            @Override
            public String doSomethingElse() {
                return "I'm here";
            }
        });
        System.out.println(worker.doSomething());
    }

}
