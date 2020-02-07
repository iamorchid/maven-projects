import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.TopicExistsException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaAdminClient {

    public static void main(String[] args) throws Exception {
        Properties kafkaProps = new Properties();
        kafkaProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "kafka9001.eniot.io:9092,kafka9002.eniot.io:9092");
        kafkaProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 50000);

        AdminClient adminClient = AdminClient.create(kafkaProps);
        adminClient.describeCluster();
        Collection<TopicListing> topicListings = adminClient.listTopics().listings().get();
        System.out.println("total topics: " + topicListings.size());

        List<NewTopic> newTopics = new ArrayList<>();
        newTopics.add(new NewTopic("IOT_HUB_BROKER_UP_METHOD_SLOW", 8, (short)1));
        adminClient.createTopics(newTopics).values().forEach((topic, future) -> {
            future.whenComplete((t, throwable) -> {
                if (throwable == null) {
                    System.out.println("successfully created: " + topic);
                } else {
                    if (isCausedBy(throwable, TopicExistsException.class)) {
                        System.out.println("topic " + topic + " already exists");
                    } else {
                        System.out.println("failed to create: " + topic + ", error: " + throwable);
                    }
                }
            });
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {

            }
        });

    }

    private static boolean isCausedBy(Throwable error, Class<?> target) {
        if (target.isAssignableFrom(error.getClass())) {
            return true;
        }
        return error.getCause() != null && isCausedBy(error.getCause(), target);
    }
}
