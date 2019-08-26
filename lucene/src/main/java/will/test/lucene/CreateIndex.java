package will.test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class CreateIndex {

    public static void main(String args[]) throws Exception {
        String indexPathname = "C:\\Users\\jian.zhang4\\lucene-data";
        Directory indexDir = FSDirectory.open(Paths.get(new File(indexPathname).toURI()));

        List<IndexCommit> commits;
        try {
            commits = DirectoryReader.listCommits(indexDir);
        } catch (IndexNotFoundException e) {
            commits = Collections.emptyList();
        }
        System.out.println("commit count: " + commits.size());
        for (IndexCommit commit : commits) {
            System.out.println("commit data: " + commit.getUserData() + ", file names: " + commit.getFileNames());
        }

        if (commits.size() > 0) {
            SegmentInfos infos = SegmentInfos.readLatestCommit(indexDir);
            System.out.println("latest infos:");
            infos.iterator().forEachRemaining(info -> System.out.println("    commit info: " + info.info.name + ", " + info.info.files()));
        }

        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriterConfig.setCommitOnClose(false);

        // 观察目录下的文件，分析该参数如何影响索引文件的生成
        // Understand how these two setttings affect lucene flush in-memory buffer
        // indexWriterConfig.setRAMBufferSizeMB(100);
        indexWriterConfig.setMaxBufferedDocs(500);

        // Keep all commits
        indexWriterConfig.setIndexDeletionPolicy(new IndexDeletionPolicy() {
            @Override
            public void onInit(List<? extends IndexCommit> list) throws IOException {

            }

            @Override
            public void onCommit(List<? extends IndexCommit> list) throws IOException {

            }
        });


        IndexWriter writer = new IndexWriter(indexDir, indexWriterConfig);
        IndexReader reader = DirectoryReader.open(writer);

        for (int i = 0; i < 1000; i ++) {
            Document document = new Document();

            document.add(new TextField("customId", "customer-" + i, Field.Store.YES));
            document.add(new TextField("test", "NRTApi", Field.Store.YES));
            writer.addDocument(document);
        }

        System.out.println("before reopen: docCount = " + searchIndex(reader, "test", "NRTApi").size());

        // Note that the logic here would flush in-memory buffer into a new segment on disk.
        // However, this would not cause creating a new commit point (while IndexWriter is
        // making changes, nothing is visible to any IndexReader searching the index, until
        // you commit or open a new NRT reader.).
        IndexReader newReader = DirectoryReader.openIfChanged((DirectoryReader)reader, writer);
        if (newReader != null) {
            System.out.println("index changed");
            reader.close();
            reader = newReader;
        }

        System.out.println("after reopen: docCount = " + searchIndex(reader, "test", "NRTApi").size());

        Map<String, String> data = new HashMap<>();
        data.put("GEN", String.valueOf(commits.size() + 1));
        writer.setLiveCommitData(data.entrySet());

        writer.commit();
        writer.close();
    }

    public static List<Document> searchIndex(IndexReader indexReader, String inField, String queryString) throws Exception {
        Query query = new QueryParser(inField, new StandardAnalyzer())
                .parse(queryString);

        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 100000);
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }

        return documents;
    }
}
