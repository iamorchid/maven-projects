package will.test;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * refer to https://blog.csdn.net/lijzheng/article/details/23618365.
 */
public class ProcMapsParser {

    @AllArgsConstructor
    @Getter
    static class Region {
        private long length;
        private String mode;
        private boolean anonMapped;

        private boolean isDummyRegion() {
            return mode.startsWith("--");
        }

        private boolean isAnnoRwRegion() {
            return anonMapped && mode.startsWith("rw");
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(new File("C:\\Users\\jian.zhang4\\will-tests\\java\\src\\main\\resources\\hub_maps").toPath());
        List<Region> regions = lines.stream().map(ProcMapsParser::parserLine).collect(Collectors.toList());

        Long total = regions.stream().filter(r -> !r.isDummyRegion()).mapToLong(Region::getLength).sum() / (1024 * 1024);
        System.out.println("total: " + total);

        Long annoRwTotal = regions.stream().filter(Region::isAnnoRwRegion).mapToLong(Region::getLength).sum() / (1024 * 1024);
        System.out.println("annoRwTotal: " + annoRwTotal);

        Long fileMappedTotal = regions.stream().filter(r -> !r.isDummyRegion() && !r.isAnonMapped()).mapToLong(Region::getLength).sum() / (1024 * 1024);
        System.out.println("fileMappedTotal: " + fileMappedTotal);
    }


    private static Region parserLine(String line) {
        List<String> splits = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(line);
        Preconditions.checkArgument(splits.size() >= 5, line);
        if (splits.size() == 5) {
            Preconditions.checkArgument(splits.get(4).equals("0"), line);
        }

        List<String> addrRange = Splitter.on("-").omitEmptyStrings().trimResults().splitToList(splits.get(0));
        Preconditions.checkArgument(addrRange.size() == 2, splits.get(0));
        long start = Long.parseLong(addrRange.get(0), 16);
        long end = Long.parseLong(addrRange.get(1), 16);

        if (splits.get(1).startsWith("--") && !splits.get(4).equals("0")) {
            System.out.println(line);
        }

        return new Region(end - start, splits.get(1), splits.get(4).equals("0"));
    }

}
