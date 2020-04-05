package will.test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DocValuesTest {

    static final String NUMERIC_FIELD = "numeric";
    static final String BINARY_FIELD = "binary";
    static final String SORTED_FIELD = "sorted";
    static final String SORTEDSET_FIELD = "sortedset";

    static long[] numericVals = new long[] {12, 13, 0, 100, 19};
    static String[] binary = new String[] {"lucene", "doc", "value", "test", "example"};
    static String[] sortedVals = new String[] {"Lucene", "facet", "abacus", "SEARCH", null};

    /**
     * 注意重复的值不会保存在doc values中（因为是sorted set）
     */
    static String[][] sortedSetVals = new String[][] {{"lucene", "search", "lucene", "search", "will"}, {"search"}, {"facet", "abacus", "search"}, {}, {}};

    static IndexReader topReader;
    static LeafReader leafReader;

    public static void main(String[] args) throws IOException {
        String indexPathname = "C:\\Users\\jian.zhang4\\lucene-data";
        Directory dir = FSDirectory.open(Paths.get(new File(indexPathname).toURI()));

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriterConfig.setCommitOnClose(false);
        indexWriterConfig.setUseCompoundFile(false);

        IndexWriter writer = new IndexWriter(dir, indexWriterConfig);
        for (int i = 0; i < numericVals.length; ++i) {
            Document doc = new Document();
            doc.add(new TextField("test", "NRTApi", Field.Store.YES));
            doc.add(new NumericDocValuesField(NUMERIC_FIELD, numericVals[i]));
            doc.add(new BinaryDocValuesField(BINARY_FIELD, new BytesRef(binary[i])));
            if (sortedVals[i] != null) {
                doc.add(new SortedDocValuesField(SORTED_FIELD, new BytesRef(sortedVals[i])));
            }
            for (String value : sortedSetVals[i]) {
                doc.add(new SortedSetDocValuesField(SORTEDSET_FIELD, new BytesRef(value)));
            }
            writer.addDocument(doc);
        }
        writer.commit();
        writer.close();

        topReader = DirectoryReader.open(dir);
        leafReader = topReader.leaves().get(0).reader();

        System.out.println("handle NUMERIC doc values");

        NumericDocValues numericDVs = leafReader.getNumericDocValues(NUMERIC_FIELD);
        while(numericDVs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            System.out.println("docId:" + numericDVs.docID() + ", value: " + numericDVs.longValue());
        }

        System.out.println("handle BINARY doc values");

        BinaryDocValues binaryDVs = leafReader.getBinaryDocValues(BINARY_FIELD);
        while(binaryDVs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            System.out.println("docId:" + binaryDVs.docID() + ", value: " + binaryDVs.binaryValue().utf8ToString());
        }

        System.out.println("handle SORTED doc values");
        SortedDocValues sortedDVs = leafReader.getSortedDocValues(SORTED_FIELD);
        System.out.println("valueCount: " + sortedDVs.getValueCount());
        while(sortedDVs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            System.out.println("docId:" + sortedDVs.docID() + ", ord: " + sortedDVs.ordValue() + ", value: " + sortedDVs.lookupOrd(sortedDVs.ordValue()).utf8ToString());
        }

        System.out.println("handle SORTED_SET doc values");
        SortedSetDocValues sortedsetDVs = leafReader.getSortedSetDocValues(SORTEDSET_FIELD);
        System.out.println("valueCount: " + sortedsetDVs.getValueCount());
        while(sortedsetDVs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            Map<Long, String> values = new HashMap<>();

            long ord;
            while((ord = sortedsetDVs.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                values.put(ord, sortedsetDVs.lookupOrd(ord).utf8ToString());
            }

            System.out.println("docId:" + sortedsetDVs.docID() + ", values: " + values);
        }
    }
}