package will.test.bytebuddy.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class AgentTest {

    public static void main(String[] args) throws Exception {
        Pojo pojo = new Pojo("jimmy", 20);
        System.out.println("pojo: " + pojo);

        AnnotatedPojo annotatedPojo = new AnnotatedPojo("vincent", 25);
        System.out.println("annotatedPojo: " + annotatedPojo);

        DataLoader loader = new DataLoader();
        loader.load();
        loader.load();
        loader.load();
    }

    @AllArgsConstructor
    @Getter
    public static class Pojo {
        private String name;
        private int age;
    }

    @AllArgsConstructor
    @Getter
    @ToJsonString
    public static class AnnotatedPojo {
        private String name;
        private int age;
    }

    public static class DataLoader {

        @StatDuration
        public void load() throws Exception {
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

}
