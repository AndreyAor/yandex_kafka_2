package ru.practice.kafka;

import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

public class BlockFilterTransformer implements ValueTransformerWithKey<String, String, String> {

    private KeyValueStore<String, String> blockedUsersStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        this.blockedUsersStore =
                (KeyValueStore<String, String>) context.getStateStore(AppConfig.BLOCKED_USERS_STORE);
    }

    @Override
    public String transform(String readOnlyKey, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        ChatMessage message = ChatMessage.fromLine(value);
        String storeKey = buildStoreKey(message.getReceiver(), message.getSender());

        String blockFlag = blockedUsersStore.get(storeKey);

        if (blockFlag != null) {
            System.out.println("MESSAGE DROPPED BY BLOCK RULE: " + value);
            return null;
        }

        System.out.println("MESSAGE PASSED BLOCK FILTER: " + value);
        return value;
    }

    @Override
    public void close() {
        // Ничего не закрываем вручную
    }

    private String buildStoreKey(String receiver, String sender) {
        return receiver + "|" + sender;
    }
}
