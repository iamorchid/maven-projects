package will.test.morphia;

//import com.mongodb.MongoClient;
//import dev.morphia.Datastore;
//import dev.morphia.Morphia;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

public class Main {

    public static void main(String[] args) {
//        final Morphia morphia = new Morphia();
//        morphia.mapPackage("will.test.morphia");
//
//        final Datastore datastore = morphia.createDatastore(new MongoClient("10.27.20.177", 27017), "test");
//        datastore.ensureIndexes();
//
//        datastore.save(new Employee("iot", "will", 1200));

        final Morphia morphia = new Morphia();
        morphia.mapPackage("will.test.morphia");

        final Datastore datastore = morphia.createDatastore(new MongoClient("10.27.20.177", 27017), "test");
        datastore.ensureIndexes();

        datastore.save(new Employee("iot", "will", 1200));
        datastore.save(new Employee("iot", "will", 1200));
    }

}
