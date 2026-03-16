package ru.practice.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class BlockedUsersProducer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", AppConfig.BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            send(producer, new BlockEvent("bob", "alice", true));
            send(producer, new BlockEvent("alice", "david", false));

            producer.flush();
            System.out.println("Block events sent to topic: " + AppConfig.BLOCKED_USERS_TOPIC);
        }
    }

    private static void send(KafkaProducer<String, String> producer, BlockEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(AppConfig.BLOCKED_USERS_TOPIC, event.getUser(), event.toLine());

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Sent block event: " + event.toLine()
                        + " to partition=" + metadata.partition()
                        + ", offset=" + metadata.offset());
            } else {
                System.err.println("Failed to send block event: " + event.toLine());
                exception.printStackTrace();
            }
        });
    }
}
