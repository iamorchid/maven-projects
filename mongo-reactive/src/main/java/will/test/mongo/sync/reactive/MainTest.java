package will.test.mongo.sync.reactive;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;

public class MainTest {

    public static void main(String[] args) throws Exception {
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

        CountDownLatch latch = new CountDownLatch(1);

        ClientSessionOptions options = ClientSessionOptions.builder().causallyConsistent(true).build();
        mongoClient.startSession(options).subscribe(new Subscriber<ClientSession>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe");
                s.request(1);
            }

            @Override
            public void onNext(ClientSession clientSession) {
                System.out.println("onNext");
                clientSession.startTransaction();

                SingleResultSubscriber<InsertOneResult> subscriber1 = new SingleResultSubscriber<>();
                database.getCollection("class").insertOne(clientSession, getClassDoc("test5")).subscribe(subscriber1);
                subscriber1.waitForDone();


                SingleResultSubscriber<InsertOneResult> subscriber2 = new SingleResultSubscriber<>();
                database.getCollection("class").insertOne(clientSession, getClassDoc("test6")).subscribe(subscriber2);
                subscriber2.waitForDone();

                SingleResultSubscriber<Void> subscriber3 = new SingleResultSubscriber<>();
                clientSession.abortTransaction().subscribe(subscriber3);
                subscriber3.waitForDone();

                clientSession.close();
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError: " + t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
                latch.countDown();
            }
        });

        latch.await();
    }

    private static Document getClassDoc(String name) {
        Document doc = new Document();
        doc.put("name", name);
        return doc;
    }

    private static class SingleResultSubscriber<T> implements Subscriber<T> {
        private CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onSubscribe(Subscription s) {
            s.request(1);
        }

        @Override
        public void onNext(T t) {
            System.out.println("result: " + t);
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
            latch.countDown();
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }

        public void waitForDone() {
            try {
                latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
