import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class KafkaReader {

    public static void main(String[] args) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "kafka9001.eniot.io:9092,kafka9002.eniot.io:9092");
        kafkaProps.put("group.id", "dev-will");
        kafkaProps.put("enable.auto.commit", "true");
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);
        BlockingQueue<ConsumerRecord<String, String>> queue = new LinkedBlockingQueue<>();

        Thread thread = new Thread(() -> {
            while(true) {
                try {
                    ConsumerRecord<String, String> record = queue.take();
                    System.out.println("record: \n" + record.offset() + ": " + record.value());
                } catch (Exception e) {
                    e.getMessage();
                }
            }
        });
        thread.start();

        String topic = "MEASURE_POINT_ORIGIN_OFFLINE_o15535059999891";
        consumer.subscribe(Arrays.asList(topic), new ConsumerRebalanceListener() {
//        consumer.subscribe(Pattern.compile("MEASURE_POINT_CAL_(?!OFFLINE).*"), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
                System.out.println("onPartitionsRevoked: " + revokedPartitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
                System.out.println("onPartitionsAssigned: " + assignedPartitions);
//                consumer.seekToEnd(assignedPartitions);
            }
        });

        while(true) {
            ConsumerRecords<String, String> result = consumer.poll(Duration.ofMillis(500));
            if (result != null && !result.isEmpty()) {
                System.out.println("fetch record count: " + result.count());

                for (ConsumerRecord<String, String> record : result) {
                    queue.add(record);
                }
            }
        }
    }
}
