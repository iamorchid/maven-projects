package will.test.mongo.sync;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ChangeStreamTest {

    public static void main(String[] args) throws Exception {
        MongoClientOptions.Builder builder = MongoClientOptions.builder()
                .socketTimeout(5_000)
                .connectionsPerHost(20)
                .cursorFinalizerEnabled(true)
//                .addCommandListener(new CommandListener() {
//                    @Override
//                    public void commandStarted(CommandStartedEvent event) {
//                        System.out.println("command: " + event.getCommand().toJson());
//                    }
//
//                    @Override
//                    public void commandSucceeded(CommandSucceededEvent event) {
//                        System.out.println("response: " + event.getResponse().toJson());
//                    }
//
//                    @Override
//                    public void commandFailed(CommandFailedEvent event) {
//
//                    }
//                })
                .readPreference(ReadPreference.primary());

        MongoClientURI uri = new MongoClientURI(
                "mongodb://172.20.17.227:27017/student?replicaSet=rs0&authSource=student",
                builder
        );

        MongoClient client = new MongoClient(uri);
        MongoDatabase db = client.getDatabase("student");
        System.out.println(db.listCollectionNames().first());

        boolean closed = false;
        BsonDocument filter = BsonDocument.parse("{$match: { \"ns.coll\": {$regex: /^class/}}}");
        MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = db//.getCollection("class")
                .watch(Collections.singletonList(filter))
                .resumeAfter(BsonDocument.parse("{\"_data\": \"825F266303000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266303FF7E423EBC0E41FB0004\"}"))
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .cursor();

        while (!closed) {
            ChangeStreamDocument<Document> document = cursor.tryNext();
            if (document != null) {
                System.out.println(document.toString());
            } else {
                System.out.println("null");
                TimeUnit.MILLISECONDS.sleep(200);
            }
        }
        client.close();
    }

}
