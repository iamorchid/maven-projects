package will.test;

public class Main {

    public static void main(String[] args) throws Exception {
        System.getenv().forEach((key, value) -> System.out.println(key + ": " + value));
        Thread.sleep(300000);
    }
}
