package ru.practice.kafka;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.state.KeyValueStore;

public class BlockEventProcessor extends AbstractProcessor<String, String> {

    private KeyValueStore<String, String> blockedUsersStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(org.apache.kafka.streams.processor.ProcessorContext context) {
        super.init(context);
        this.blockedUsersStore =
                (KeyValueStore<String, String>) context.getStateStore(AppConfig.BLOCKED_USERS_STORE);
    }

    @Override
    public void process(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        BlockEvent event = BlockEvent.fromLine(value);

        String storeKey = buildStoreKey(event.getUser(), event.getBlockedUser());

        if (event.isBlocked()) {
            blockedUsersStore.put(storeKey, "BLOCKED");
            System.out.println("BLOCK STORE UPDATE: added " + storeKey);
        } else {
            blockedUsersStore.delete(storeKey);
            System.out.println("BLOCK STORE UPDATE: removed " + storeKey);
        }
    }

    private String buildStoreKey(String user, String blockedUser) {
        return user + "|" + blockedUser;
    }
}
