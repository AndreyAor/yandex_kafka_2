package ru.practice.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class BannedWordsProducer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", AppConfig.BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            send(producer, new BannedWordEvent("spam", true));
            send(producer, new BannedWordEvent("badword", true));

            producer.flush();
            System.out.println("Banned word events sent to topic: " + AppConfig.BANNED_WORDS_TOPIC);
        }
    }

    private static void send(KafkaProducer<String, String> producer, BannedWordEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(AppConfig.BANNED_WORDS_TOPIC, event.getWord(), event.toLine());

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Sent banned word event: " + event.toLine()
                        + " to partition=" + metadata.partition()
                        + ", offset=" + metadata.offset());
            } else {
                System.err.println("Failed to send banned word event: " + event.toLine());
                exception.printStackTrace();
            }
        });
    }
}
