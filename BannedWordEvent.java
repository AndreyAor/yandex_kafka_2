package ru.practice.kafka;

public class BannedWordEvent {

    private final String word;
    private final boolean active;

    public BannedWordEvent(String word, boolean active) {
        this.word = word;
        this.active = active;
    }

    public String getWord() {
        return word;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Формат:
     * word|action
     *
     * Примеры:
     * badword|ADD
     * badword|REMOVE
     */
    public static BannedWordEvent fromLine(String line) {
        String[] parts = line.split("\\|", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid banned word event: " + line);
        }

        boolean active = "ADD".equalsIgnoreCase(parts[1]);

        return new BannedWordEvent(parts[0], active);
    }

    public String toLine() {
        return word + "|" + (active ? "ADD" : "REMOVE");
    }

    @Override
    public String toString() {
        return "BannedWordEvent{" +
                "word='" + word + '\'' +
                ", active=" + active +
                '}';
    }
}
