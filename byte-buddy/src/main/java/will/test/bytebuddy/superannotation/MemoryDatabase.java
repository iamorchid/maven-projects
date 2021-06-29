package will.test.bytebuddy.superannotation;

import java.util.Arrays;
import java.util.List;

public class MemoryDatabase {

    public List<String> load(String info) {
        return Arrays.asList(this + "> " + info + ": foo", info + ": bar");
    }

}
