package ru.practice.kafka;

public class BlockEvent {

    private final String user;
    private final String blockedUser;
    private final boolean blocked;

    public BlockEvent(String user, String blockedUser, boolean blocked) {
        this.user = user;
        this.blockedUser = blockedUser;
        this.blocked = blocked;
    }

    public String getUser() {
        return user;
    }

    public String getBlockedUser() {
        return blockedUser;
    }

    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Формат:
     * user|blockedUser|action
     *
     * Примеры:
     * bob|alice|BLOCK
     * bob|alice|UNBLOCK
     */
    public static BlockEvent fromLine(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid block event: " + line);
        }

        boolean blocked = "BLOCK".equalsIgnoreCase(parts[2]);

        return new BlockEvent(parts[0], parts[1], blocked);
    }

    public String toLine() {
        return user + "|" + blockedUser + "|" + (blocked ? "BLOCK" : "UNBLOCK");
    }

    @Override
    public String toString() {
        return "BlockEvent{" +
                "user='" + user + '\'' +
                ", blockedUser='" + blockedUser + '\'' +
                ", blocked=" + blocked +
                '}';
    }
}
