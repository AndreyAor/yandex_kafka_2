package ru.practice.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ChatMessageProducer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", AppConfig.BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            send(producer, new ChatMessage("msg-1", "alice", "bob", "hello bob"));
            send(producer, new ChatMessage("msg-2", "alice", "bob", "this is spam text"));
            send(producer, new ChatMessage("msg-3", "charlie", "bob", "badword should be hidden"));
            send(producer, new ChatMessage("msg-4", "david", "alice", "normal message for alice"));

            producer.flush();
            System.out.println("Test chat messages sent to topic: " + AppConfig.MESSAGES_TOPIC);
        }
    }

    private static void send(KafkaProducer<String, String> producer, ChatMessage message) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(AppConfig.MESSAGES_TOPIC, message.getReceiver(), message.toLine());

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Sent chat message: " + message.toLine()
                        + " to partition=" + metadata.partition()
                        + ", offset=" + metadata.offset());
            } else {
                System.err.println("Failed to send chat message: " + message.toLine());
                exception.printStackTrace();
            }
        });
    }
}
