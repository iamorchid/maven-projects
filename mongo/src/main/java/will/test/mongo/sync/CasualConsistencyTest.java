package will.test.mongo.sync;

import com.mongodb.*;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

/**
 * https://docs.mongodb.com/manual/core/read-isolation-consistency-recency/#causal-consistency-examples
 */
public class CasualConsistencyTest {

    public static void main(String[] args) {
        MongoClientOptions.Builder builder = MongoClientOptions.builder()
                .socketTimeout(5_000)
                .connectionsPerHost(20)
                .cursorFinalizerEnabled(true)
                .addCommandListener(new CommandListener() {
                    @Override
                    public void commandStarted(CommandStartedEvent event) {
                        System.out.println("command: " + event.getCommand().toJson());
                    }

                    @Override
                    public void commandSucceeded(CommandSucceededEvent event) {
                        System.out.println("response: " + event.getResponse().toJson());
                    }

                    @Override
                    public void commandFailed(CommandFailedEvent event) {

                    }
                })
                .readPreference(ReadPreference.primary());

        MongoClientURI uri = new MongoClientURI(
                "mongodb://eternity:27017/?replicaSet=rs0",
                builder
        );

        MongoClient client = new MongoClient(uri);
        final Bson filter = eq("classId", 101);

        ClientSessionOptions sessionOptions = ClientSessionOptions.builder()
                .causallyConsistent(true)
                .defaultTransactionOptions(TransactionOptions.builder()
                        .readPreference(ReadPreference.secondaryPreferred())
                        .writeConcern(WriteConcern.MAJORITY)
                        .readConcern(ReadConcern.MAJORITY)
                        .build())
                .build();
        ClientSession session1 = client.startSession(sessionOptions);

        // 下面这些配置和session中配置的关系是什么？到底用那个？
        // 参见com.mongodb.client.internal.MongoClientDelegate.DelegateOperationExecutor#getReadWriteBinding
        MongoCollection<Document> items = client.getDatabase("student")
                .withReadPreference(ReadPreference.secondaryPreferred())
                .withReadConcern(ReadConcern.LOCAL)
                .withWriteConcern(WriteConcern.ACKNOWLEDGED)
                .getCollection("class");

        // 显式开启TX时，必须使用primary read preference，否则报错。因此，在显示使用TX时，我们总会从primary读取数据。
        // 其实这个也很合理，TX信息只会放在primary上，而且没有commit之前，改动没道理会通过oplog同步到副本，如果事务过程
        // 中从replica执行读取操作，这个显然不合理。
        //
        // 事务显示开启后，如果enable了casually consistent特性，那么在提交事务中的第一个请求且session中定义了operationTime时,
        // 才会附带afterClusterTime信息，后续请求则不会附带afterClusterTime，可以参见CommandMessage#getExtraElements方法。
        //
        // 事务没有显示开启的话，如果session开启了casually consistent特性，每个读取请求总会带上afterClusterTime（当然，session
        // 中已经定义了operationTime），可以参见OperationReadConcernHelper#OperationReadConcernHelper方法。
        //
        // 关于read concern，可以阅读https://blog.csdn.net/weixin_34334744/article/details/89593952
        session1.startTransaction(TransactionOptions.builder()
                // 显式开启TX时，必须使用primary read preference
                .readPreference(ReadPreference.primary())
                .writeConcern(WriteConcern.W1)
                // 这有没有可能读取不到同一个事务中的上一次写操作（比如写操作还没有同步到majority）？但事务没有
                // 提交之前，又怎么可能同步到其他的replicas呢？因此，我的理解是，同一个事务中，读取操作总是能
                // 之前的更新操作的。这里的read concern针对的是，读取这个事务之外的更新操作。
                .readConcern(ReadConcern.MAJORITY)
                .build());
        items.insertOne(session1, new Document("name", "Computer Science").append("classId", 101));
        for (Document item: items.find(session1, filter)) {
            System.out.println(item);
        }
        session1.commitTransaction();

        System.out.println("-----------------------------------------------");

        ClientSession session2 = client.startSession(ClientSessionOptions.builder().causallyConsistent(true).build());
        session2.advanceClusterTime(session1.getClusterTime());
        session2.advanceOperationTime(session1.getOperationTime());

        items = client.getDatabase("student")
                // .withReadPreference(ReadPreference.secondary())
                // I don't have secondary for test
                .withReadPreference(ReadPreference.secondaryPreferred())
                .withReadConcern(ReadConcern.MAJORITY)
                .withWriteConcern(WriteConcern.MAJORITY.withWTimeout(1000, TimeUnit.MILLISECONDS))
                .getCollection("class");

        for (Document item: items.find(session2, filter)) {
            System.out.println(item);
        }

        System.out.println(items.deleteMany(session2, filter));
    }

}
