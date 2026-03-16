package ru.practice.kafka;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.state.KeyValueStore;

public class BannedWordEventProcessor extends AbstractProcessor<String, String> {

    private KeyValueStore<String, String> bannedWordsStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(org.apache.kafka.streams.processor.ProcessorContext context) {
        super.init(context);
        this.bannedWordsStore =
                (KeyValueStore<String, String>) context.getStateStore(AppConfig.BANNED_WORDS_STORE);
    }

    @Override
    public void process(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        BannedWordEvent event = BannedWordEvent.fromLine(value);
        String normalizedWord = event.getWord().toLowerCase();

        if (event.isActive()) {
            bannedWordsStore.put(normalizedWord, "ACTIVE");
            System.out.println("BANNED WORD STORE UPDATE: added " + normalizedWord);
        } else {
            bannedWordsStore.delete(normalizedWord);
            System.out.println("BANNED WORD STORE UPDATE: removed " + normalizedWord);
        }
    }
}
