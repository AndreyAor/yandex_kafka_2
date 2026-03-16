package ru.practice.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Properties;

public class ModerationStreamsApp {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, AppConfig.STREAMS_APP_ID + "-moderation");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.STATE_DIR_CONFIG, "kafka-streams-state");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 1);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        props.put(StreamsConfig.consumerPrefix("auto.offset.reset"), "latest");

        StreamsBuilder builder = new StreamsBuilder();

        StoreBuilder<KeyValueStore<String, String>> blockedUsersStoreBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(AppConfig.BLOCKED_USERS_STORE),
                        Serdes.String(),
                        Serdes.String()
                );

        StoreBuilder<KeyValueStore<String, String>> bannedWordsStoreBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(AppConfig.BANNED_WORDS_STORE),
                        Serdes.String(),
                        Serdes.String()
                );

        builder.addStateStore(blockedUsersStoreBuilder);
        builder.addStateStore(bannedWordsStoreBuilder);

        builder.stream(
                        AppConfig.BLOCKED_USERS_TOPIC,
                        Consumed.with(Serdes.String(), Serdes.String())
                )
                .process(
                        BlockEventProcessor::new,
                        Named.as("blocked-users-processor"),
                        AppConfig.BLOCKED_USERS_STORE
                );

        builder.stream(
                        AppConfig.BANNED_WORDS_TOPIC,
                        Consumed.with(Serdes.String(), Serdes.String())
                )
                .process(
                        BannedWordEventProcessor::new,
                        Named.as("banned-words-processor"),
                        AppConfig.BANNED_WORDS_STORE
                );

        builder.stream(
                        AppConfig.MESSAGES_TOPIC,
                        Consumed.with(Serdes.String(), Serdes.String())
                )
                .transformValues(
                        BlockFilterTransformer::new,
                        Named.as("block-filter-transformer"),
                        AppConfig.BLOCKED_USERS_STORE
                )
                .filter((key, value) -> value != null, Named.as("drop-blocked-messages"))
                .transformValues(
                        CensorshipTransformer::new,
                        Named.as("censorship-transformer"),
                        AppConfig.BANNED_WORDS_STORE
                )
                .filter((key, value) -> value != null, Named.as("drop-null-after-censorship"))
                .to(
                        AppConfig.FILTERED_MESSAGES_TOPIC,
                        Produced.with(Serdes.String(), Serdes.String())
                );

        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        streams.start();
        System.out.println("ModerationStreamsApp started.");
    }
}
