package will.test.mongo.sync.reactive.service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import will.test.mongo.sync.reactive.data.ClassInfo;

import java.util.Date;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class SyncDataService implements CommandLineRunner {

    @Autowired
    private ReactiveMongoOperations reactiveTemplate;

    @Override
    public void run(String... args) throws Exception {
        testChangeStreamForDatabaseWithoutSpringTemplate();
    }

    /**
     * TODO 链接同一个集群的多个database，会在本地维持多个cluaster信息吗？
     */
    private void testChangeStreamForDatabaseWithoutSpringTemplate() {
        ConnectionString connString = new ConnectionString(
                "mongodb://eternity:27017/student?replicaSet=rs0&waitqueuetimeoutms=3000&sockettimeoutms=3000"
        );
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .readPreference(ReadPreference.primaryPreferred())
                // 如果需要通过cursor多次读取（即涉及到访问具体server维护的session），则不能简单在网络连接层retry read.
                // （具体用法见com.mongodb.operation.CommandOperationHelper#createCommandCallback），而需要具体的
                // operation进行显示的retry（见AsyncChangeStreamBatchCursor#resumeableOperation处理failure的情况）。
                .retryReads(false)
                .retryWrites(false)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);

        MongoDatabase database = mongoClient.getDatabase("student");

        database.watch()
                .resumeAfter(BsonDocument.parse("{\"_data\": \"825F266303000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266303FF7E423EBC0E41FB0004\"}"))
                .batchSize(10)
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .subscribe(new Subscriber<ChangeStreamDocument<Document>>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(ChangeStreamDocument<Document> documentChangeStreamDocument) {
                        System.out.println("received: " + documentChangeStreamDocument);
                    }

                    @Override
                    public void onError(Throwable t) {
                        // TODO check the error type
                        t.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // TODO this is un-expected and we need to retry
                        System.out.println("change stream has completed unexpectedly");
                    }
                });
    }

    /**
     * TODO 1. how to control the batch size
     * TODO 2. how to support multiple ReactiveMongoOperations
     */
    private void testChangeStreamForDatabase() {
        Flux<ChangeStreamEvent<ClassInfo>> changeStreamEventFlux = reactiveTemplate.changeStream(
                null, null,
                ChangeStreamOptions.builder()
                        .resumeAfter(BsonDocument.parse("{\"_data\": \"825F266303000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266303FF7E423EBC0E41FB0004\"}"))
                        .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                        .build(),
                ClassInfo.class);
        changeStreamEventFlux.subscribe(changeStreamEvent -> {
            System.out.println("received event: " + changeStreamEvent);

            // Spring would do the type conversion for us here if we have full document for this event
            System.out.println("body: " + changeStreamEvent.getBody());
        }, error -> {
            System.out.println("date:" + new Date() + ", error: " + error);

            if (error instanceof MongoCommandException) {
                if (((MongoCommandException) error).getErrorLabels().contains("NonResumableChangeStreamError")) {
                    // fatal error, raise alarm
                    // com.mongodb.MongoCommandException: Command failed with error 280 (ChangeStreamFatalError): 'cannot resume stream; the resume token was not found. {_data: "825F266370000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266370FF7E423EBC0E41FC0004"}' on server eternity:27017. The full response is {"errorLabels": ["NonResumableChangeStreamError"], "operationTime": {"$timestamp": {"t": 1596531407, "i": 1}}, "ok": 0.0, "errmsg": "cannot resume stream; the resume token was not found. {_data: \"825F266370000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266370FF7E423EBC0E41FC0004\"}", "code": 280, "codeName": "ChangeStreamFatalError", "$clusterTime": {"clusterTime": {"$timestamp": {"t": 1596531407, "i": 1}}, "signature": {"hash": {"$binary": {"base64": "AAAAAAAAAAAAAAAAAAAAAAAAAAA=", "subType": "00"}}, "keyId": 0}}}
                    return;
                }
            }

            // TODO retry and if it fails after some times, raise an alarm
            error.printStackTrace();
        }, () -> {
            // TODO raise an alarm
            System.out.println("change stream has completed");
        });
    }

    private void testChangeStreamForCollection() {
        Flux<ChangeStreamEvent<Document>> changeStreamEventFlux = reactiveTemplate.changeStream(
                "class",
                ChangeStreamOptions.builder()
                        .filter(newAggregation(match(where("operationType").is("insert"))))
                        // invalidate event之前的某个token
                        .resumeAfter(BsonDocument.parse("{\"_data\": \"825F266303000000012B022C0100296E5A100432BBB7147D7E4EE39D29C3B1A34CAD5746645F696400645F266303FF7E423EBC0E41FB0004\"}"))
//                        // 下面的token指向某个invalidate event（如drop当前被watched collection）
//                        .resumeAfter(BsonDocument.parse("{\"_data\": \"825F28C40C000000012B022C0100296F5A100432BBB7147D7E4EE39D29C3B1A34CAD5704\"}"))
                        .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                        .build(),
                Document.class);
        changeStreamEventFlux.subscribe(changeStreamEvent -> {
            System.out.println("received event: " + changeStreamEvent);
        }, error -> {
            System.out.println("date:" + new Date() + ", error: " + error);
            error.printStackTrace();
        }, () -> {
            System.out.println("change stream has completed");
        });
    }
}
