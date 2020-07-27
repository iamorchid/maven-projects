import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.v1.*;

@Slf4j
public class Main {

    public static void main(String[] args) {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "test"));
        log.info("neo4j connection init success");

        try(Session session = driver.session(AccessMode.WRITE)) {
            Transaction tx = session.beginTransaction();

            StatementResult result0 = tx.run("match (n: Node) return n");
            log.info("result1: {}", result0.list());

            tx.run("create (n:Node{department: \"FID\", role:\"ED\", name: \"alex\"})");
            StatementResult result1 = tx.run("match (n: Node) return n");
            log.info("result1: {}", result1.list());

            tx.run("create (n:Node{department: \"FID\", role:\"VP\", name: \"boz\"})");
            StatementResult result2 = tx.run("match (n: Node) return n");
            log.info("result2: {}", result2.list());

            tx.failure();
        }

        driver.close();
    }

}
