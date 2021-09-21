import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class KafkaReader {

    public static void main(String[] args) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "localhost:9092");
        kafkaProps.put("group.id", "dev-will");
        kafkaProps.put("enable.auto.commit", "true");
        kafkaProps.put("key.deserializer", StringDeserializer.class.getName());
        kafkaProps.put("value.deserializer", StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);
        BlockingQueue<ConsumerRecord<String, String>> queue = new LinkedBlockingQueue<>();

        Thread thread = new Thread(() -> {
            while(true) {
                try {
                    ConsumerRecord<String, String> r = queue.take();
                    System.out.println("partition: " + r.partition() + ", offset: " + r.offset() + ", value: " + r.value());
                } catch (Exception e) {
                    e.getMessage();
                }
            }
        });
        thread.start();

        String topic = "test-data";
        consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
                System.out.println("onPartitionsRevoked: " + revokedPartitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
                System.out.println("onPartitionsAssigned: " + assignedPartitions);
            }
        });

        while(true) {
            ConsumerRecords<String, String> result = consumer.poll(Duration.ofMillis(1000));
            if (result != null && !result.isEmpty()) {
                for (ConsumerRecord<String, String> record : result) {
                    queue.add(record);
                }
            }
        }
    }
}
