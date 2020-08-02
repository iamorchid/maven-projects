package will.test.morphia;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

public class ChangeStreamTest {

    public static void main(String[] args) throws Exception {
        MongoClientOptions.Builder builder = MongoClientOptions.builder()
                .socketTimeout(5_000)
                .connectionsPerHost(20)
                .cursorFinalizerEnabled(true)
                .readPreference(ReadPreference.primary());

        MongoClientURI uri = new MongoClientURI(
                "mongodb://172.20.17.227:27017/student?replicaSet=rs0&authSource=student",
                builder
        );

        MongoClient client = new MongoClient(uri);
        MongoDatabase db = client.getDatabase("student");
        System.out.println(db.listCollectionNames().first());

        boolean closed = false;
        MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = db.getCollection("class")
                .watch()
                .resumeAfter(BsonDocument.parse("{\"_data\": \"825F266303000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266303FF7E423EBC0E41FB0004\"}"))
                .cursor();
        while(!closed) {
            ChangeStreamDocument<Document> document = cursor.tryNext();
            if (document != null) {
                System.out.println(document.toString());
            } else {
                TimeUnit.MILLISECONDS.sleep(200);
            }
        }
        client.close();
    }

}
