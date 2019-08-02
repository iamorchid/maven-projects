import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class KafkaReader {

    private static Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public static void main(String[] args) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "kafka9001.eniot.io:9092,kafka9002.eniot.io:9092");
        kafkaProps.put("group.id", "dev-will");
        kafkaProps.put("enable.auto.commit", "true");
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer consumer = new KafkaConsumer<>(kafkaProps);


        final List<TopicPartition> partitions = new LinkedList<>();

        consumer.subscribe(Arrays.asList("iot_mqtt_measurepoint_beta"), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println("onPartitionsRevoked: " + partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println("onPartitionsAssigned: " + partitions);
                for(TopicPartition partition : partitions) {
                    if (partition.partition() == 0) {
                        consumer.seek(partition, 545410480);
                    } else if (partition.partition() == 1) {
                        consumer.seek(partition, 238393002);
                    }

                    partitions.add(partition);
                }
            }
        });

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        BlockingQueue<ConsumerRecord<String, String>> queue = new LinkedBlockingQueue<>();
        boolean paused = false;

        Thread thread = new Thread(() -> {
            while(true) {
                try {
                    ConsumerRecord<String, String> record = queue.take();
                    RuleEngineMsg msg = gson.fromJson(record.value(), RuleEngineMsg.class);
                    if (Objects.equals(msg.getOrgId(), "o15475450989191")) {
                        Map<String, Object> payload = (Map<String, Object>)msg.getPayload();
                        if (Objects.equals("EGCAf4Ar", payload.get("assetId"))) {
                            double obj = (Double)payload.get("time");
                            System.out.println(msg.getPayload() + ", local-time: " + format.format(new Date(((long)obj))));
                        }
                    }
                } catch (Exception e) {
                    e.getMessage();
                }
            }
        });
        thread.start();

        while(true) {
            ConsumerRecords<String, String> result = consumer.poll(Duration.ofMillis(200));
            if (result != null && !result.isEmpty()) {
                System.out.println("fetch record count: " + result.count());

                for (ConsumerRecord<String, String> record : result) {
                    queue.add(record);
                }
            }

            if (queue.isEmpty()) {
                consumer.resume(partitions);
                paused = false;
            } else if (!paused) {
                consumer.pause(partitions);
                paused = true;
            }
        }
    }
}
