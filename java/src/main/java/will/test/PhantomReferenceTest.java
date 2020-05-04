package will.test;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This test is for understand how DirectByteBuffer is relased. <p/>
 *
 * 既然要调用System.gc，那肯定是想通过触发一次gc操作来回收堆外内存，不过我想先说的是堆外内存不会对gc
 * 造成什么影响(这里的System.gc除外)，但是堆外内存的回收其实依赖于我们的gc机制，首先我们要知道在java
 * 层面和我们在堆外分配的这块内存关联的只有与之关联的DirectByteBuffer对象了，它记录了这块内存的基地址
 * 以及大小，那么既然和gc也有关，那就是gc能通过操作DirectByteBuffer对象来间接操作对应的堆外内存了。
 * DirectByteBuffer对象在创建的时候关联了一个PhantomReference，说到PhantomReference它其实主要是
 * 用来跟踪对象何时被回收的，它不能影响gc决策，但是gc过程中如果发现某个对象除了只有PhantomReference
 * 引用它之外，并没有其他的地方引用它了，那将会把这个引用放到java.lang.ref.Reference.pending队列里，
 * 在gc完毕的时候通知ReferenceHandler这个守护线程去执行一些后置处理，而DirectByteBuffer关联的
 * PhantomReference是PhantomReference的一个子类，在最终的处理里会通过Unsafe的free接口来释放
 * DirectByteBuffer对应的堆外内存块。
 *
 */
public class PhantomReferenceTest {
    private static final ReferenceQueue<Object> queue = new ReferenceQueue();

    @AllArgsConstructor
    @Getter
    static class Persion {
        private String name;
    }

    static class Cleaner extends PhantomReference<Persion> {
        private final String objString;

        public Cleaner(Persion referent) {
            super(referent, queue);
            objString = referent.getName();
        }

        private void cleanResource() {
            System.out.println("clean resource for " + objString);
        }
    }

    private static long getDirectBufferSize() {
        try {
            Field field = Class.forName("java.nio.Bits").getDeclaredField("reservedMemory");
            field.setAccessible(true);
            AtomicLong directMemoryUsed = (AtomicLong) field.get(null);
            return directMemoryUsed.get();
        } catch (Throwable e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void main(String[] args) throws Exception {
        Set<Cleaner> cleaners = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            final Persion persion = new Persion("name#" + i);
            cleaners.add(new Cleaner(persion));

            // Make use of person here
            // TODO ...
        }

        System.out.println("direct memory used: " + getDirectBufferSize());
        ByteBuffer.allocateDirect(1024 * 1024);
        System.out.println("direct memory used: " + getDirectBufferSize());

        System.gc();
        TimeUnit.SECONDS.sleep(3);

        System.out.println("after gc");

        System.out.println("direct memory used: " + getDirectBufferSize());

        Cleaner cleaner = null;
        while((cleaner = (Cleaner) queue.poll()) != null) {
            cleaner.cleanResource();
        }
    }
}
