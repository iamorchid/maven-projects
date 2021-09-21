package will.test;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibrdkafkaSymParser {

    // ./consumer(+0x68e0d) [0x563a3a234e0d]
    private static final Pattern MY_TRACE_OUT = Pattern.compile("[^(]+[(][+]0x([0-9a-zA-Z]+)[)].*");

    public static void main(String[] args) throws Exception {
        TreeSet<SymbolInfo> textSyms = new TreeSet<>();
        SymbolInfo lastSym = null;
        // nm -n a.out
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/will/projects/librdkafka/librdkafka.sym")));
        String line = null;
        while((line = reader.readLine()) != null) {
            String[] parts = line.split(" ", 3);
            if (parts.length == 3) {
                try {
                    long start = Long.parseLong(parts[0], 16);
                    if (lastSym != null) {
                        lastSym.setEndAddr(start);
                    }
                    lastSym = new SymbolInfo(start, 0, parts[2]);
                    if ("T".equalsIgnoreCase(parts[1])) {
                        textSyms.add(lastSym);
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }
        }

//        textSyms.forEach(s -> System.out.println(s.toString()));

        reader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/will/projects/librdkafka/mytrace.out")));
        while((line = reader.readLine()) != null) {
            if (line.contains(".so")) {
                System.out.println(line);
                continue;
            }
            Matcher m = MY_TRACE_OUT.matcher(line);
            if (!m.matches()) {
                System.out.println(line);
                continue;
            }
            try {
                long addr = Long.parseLong(m.group(1), 16);
                SymbolInfo sym = textSyms.floor(new SymbolInfo(addr, 0, ""));
                if (sym == null) {
                    System.out.println(line);
                } else {
                    System.out.println(line + " " + sym.symbol);
                }
            } catch (Throwable e) {
                System.out.println(line);
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class SymbolInfo implements Comparable<SymbolInfo> {
        private long startAddr;
        private long endAddr;
        private String symbol;

        @Override
        public int compareTo(SymbolInfo symbolInfo) {
            return Long.compare(startAddr, symbolInfo.startAddr);
        }
    }
}
