package ru.practice.kafka;

import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.regex.Pattern;

public class CensorshipTransformer implements ValueTransformerWithKey<String, String, String> {

    private KeyValueStore<String, String> bannedWordsStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        this.bannedWordsStore =
                (KeyValueStore<String, String>) context.getStateStore(AppConfig.BANNED_WORDS_STORE);
    }

    @Override
    public String transform(String readOnlyKey, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        ChatMessage message = ChatMessage.fromLine(value);
        String censoredText = censorText(message.getText());

        ChatMessage censoredMessage = new ChatMessage(
                message.getMessageId(),
                message.getSender(),
                message.getReceiver(),
                censoredText
        );

        if (!message.getText().equals(censoredText)) {
            System.out.println("MESSAGE CENSORED: " + value + " -> " + censoredMessage.toLine());
        } else {
            System.out.println("MESSAGE HAS NO BANNED WORDS: " + value);
        }

        return censoredMessage.toLine();
    }

    private String censorText(String text) {
        String result = text;

        try (KeyValueIterator<String, String> iterator = bannedWordsStore.all()) {
            while (iterator.hasNext()) {
                String bannedWord = iterator.next().key;

                if (bannedWord == null || bannedWord.isBlank()) {
                    continue;
                }

                String replacement = "*".repeat(bannedWord.length());
                String regex = "(?i)\\b" + Pattern.quote(bannedWord) + "\\b";

                result = result.replaceAll(regex, replacement);
            }
        }

        return result;
    }

    @Override
    public void close() {
        // Ничего не закрываем вручную
    }
}
