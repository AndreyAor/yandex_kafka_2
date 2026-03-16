package ru.practice.kafka;

public class ChatMessage {

    private final String messageId;
    private final String sender;
    private final String receiver;
    private final String text;

    public ChatMessage(String messageId, String sender, String receiver, String text) {
        this.messageId = messageId;
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getText() {
        return text;
    }

    /**
     * Формат:
     * messageId|sender|receiver|text
     *
     * Пример:
     * msg-1|alice|bob|hello bob
     */
    public static ChatMessage fromLine(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid chat message: " + line);
        }
        return new ChatMessage(parts[0], parts[1], parts[2], parts[3]);
    }

    public String toLine() {
        return messageId + "|" + sender + "|" + receiver + "|" + text;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
