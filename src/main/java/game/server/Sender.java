package game.server;

public enum Sender {
    PLAYER,
    SERVER;

    /**
     * Gets the chat prefix associated with the sender.
     * @return the prefix string
     */
    public String getPrefix() {
        return switch (this) {
            case PLAYER -> "[PLAYER]: ";
            case SERVER -> "[SERVER]: ";
        };
    }
}
