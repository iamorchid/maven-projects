package will.test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class LeafReaderTest {

    private static IndexWriterConfig getConfig() {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriterConfig.setCommitOnClose(false);
        indexWriterConfig.setMaxBufferedDocs(10);

        return indexWriterConfig;
    }

    public static void main(String[] args) throws Exception {
        Directory indexDir = new RAMDirectory();;
        IndexWriter writer = new IndexWriter(indexDir, getConfig());
        DirectoryReader reader = DirectoryReader.open(writer);

        for (int i = 0; i < 95; i ++) {
            Document document = new Document();

            document.add(new TextField("customId", "customer-" + i, Field.Store.YES));
            document.add(new TextField("test", "NRTApi", Field.Store.YES));
            writer.addDocument(document);
        }

        System.out.print("----------------before commit----------------");

        System.out.println("left count: " + reader.getContext().leaves().size());
        for (LeafReaderContext leaf : reader.getContext().leaves()) {
            System.out.println("ord: " + leaf.ord + ", docBase: " + leaf.docBase + ", maxDoc: " + leaf.reader().maxDoc());
        }

        System.out.print("----------------before commit (use NRT) ----------------");
        reader = DirectoryReader.openIfChanged(reader, writer);
        System.out.println("left count: " + reader.getContext().leaves().size());
        for (LeafReaderContext leaf : reader.getContext().leaves()) {
            System.out.println("ord: " + leaf.ord + ", docBase: " + leaf.docBase + ", maxDoc: " + leaf.reader().maxDoc());
        }

        System.out.print("----------------after commit----------------");
        writer.commit();
        writer.close();

        IndexWriter writer2 = new IndexWriter(indexDir, getConfig());
        reader = DirectoryReader.open(writer2);

        System.out.println("left count: " + reader.getContext().leaves().size());
        for (LeafReaderContext leaf : reader.getContext().leaves()) {
            System.out.println("ord: " + leaf.ord + ", docBase: " + leaf.docBase + ", maxDoc: " + leaf.reader().maxDoc());
        }

        reader.close();
    }
}
