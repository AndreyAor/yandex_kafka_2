package ru.practice.kafka;

public final class AppConfig {

    private AppConfig() {
    }

    public static final String BOOTSTRAP_SERVERS =
            "127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096";

    public static final String MESSAGES_TOPIC = "messages";
    public static final String FILTERED_MESSAGES_TOPIC = "filtered_messages";
    public static final String BLOCKED_USERS_TOPIC = "blocked_users";
    public static final String BANNED_WORDS_TOPIC = "banned_words";

    public static final String STREAMS_APP_ID = "chat-moderation-streams-app";

    public static final String BLOCKED_USERS_STORE = "blocked-users-store";
    public static final String BANNED_WORDS_STORE = "banned-words-store";
}
