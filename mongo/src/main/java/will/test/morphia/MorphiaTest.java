package will.test.morphia;

import com.mongodb.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import will.test.morphia.data.Employee;

public class MorphiaTest {

    public static void main(String[] args) {
        betaTest();
    }

    private static void betaTest() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder()
                .socketTimeout(5_000)
                .connectionsPerHost(20)
                .cursorFinalizerEnabled(true)
                .readPreference(ReadPreference.primary());

        MongoClientURI uri = new MongoClientURI(
                "mongodb://EnOS_hub_db_user:envision_terminal@mongodb-cim9001:27017,mongodb-cim9002:27017/EnOS_hub_db?replicaSet=rs1&authSource=EnOS_hub_db",
                builder
        );

        MongoClient client = new MongoClient(uri);

        final Morphia morphia = new Morphia();
        final Datastore datastore = morphia.createDatastore(client, "EnOS_hub_db");
        datastore.ensureIndexes();
        datastore.setDefaultWriteConcern(new WriteConcern(2));

        datastore.save(new Employee("iot", "tiger", 2000));

//        System.out.println(client.getServerAddressList());
//        System.out.println(client.getReplicaSetStatus());
//        System.out.println(client.listDatabaseNames().first());
    }

    private static void alphaTest() {
        final Morphia morphia = new Morphia();
        morphia.mapPackage("will.test.morphia");

        final Datastore datastore = morphia.createDatastore(new MongoClient("10.27.20.177", 27017), "test");
        datastore.ensureIndexes();
        datastore.setDefaultWriteConcern(new WriteConcern(3));

        datastore.save(new Employee("iot", "jimmy", 2000));
    }

}
