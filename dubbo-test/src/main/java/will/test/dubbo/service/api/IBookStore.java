package will.test.dubbo.service.api;

import java.util.List;

public interface IBookStore {

    List<String> books();

    List<String> getBestSellers();
}
