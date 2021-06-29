package will.test.bytebuddy.aspect.cache;

import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

public class BookStore {

    public Book get(String name) {
        return getBookInfoFromDb(name);
    }

    @SneakyThrows
    private Book getBookInfoFromDb(String name) {
        // mock the loading from database
        TimeUnit.MICROSECONDS.sleep(5_000);
        return new Book(name);
    }

}
