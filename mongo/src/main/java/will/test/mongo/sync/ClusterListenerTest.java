package will.test.mongo.sync;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ClusterClosedEvent;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;
import com.mongodb.event.ClusterOpeningEvent;

public class ClusterListenerTest {

    public static void main(String[] args) throws Exception {
        MongoClientOptions.Builder builder = MongoClientOptions.builder()
                .socketTimeout(5_000)
                .connectionsPerHost(20)
                .cursorFinalizerEnabled(true)
                .addClusterListener(new ClusterListener() {
                    @Override
                    public void clusterOpening(ClusterOpeningEvent event) {
                        System.out.println("clusterOpening");
                    }

                    @Override
                    public void clusterClosed(ClusterClosedEvent event) {
                        System.out.println("clusterClosed");
                    }

                    @Override
                    public void clusterDescriptionChanged(ClusterDescriptionChangedEvent event) {
                        System.out.println("clusterDescriptionChanged");
                    }
                })
                .readPreference(ReadPreference.primaryPreferred());

        MongoClientURI uri = new MongoClientURI(
                "mongodb://172.20.17.227:27017/student?replicaSet=rs0&authSource=student",
                builder
        );

        MongoClient client = new MongoClient(uri);
        MongoDatabase db = client.getDatabase("student");
        System.out.println(db.listCollectionNames().first());

        System.out.println(db.listCollectionNames().first());

        System.in.read();
    }
}
