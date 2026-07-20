package game.server;

public enum Sender {
    PLAYER,
    SERVER;

/**
 * Returns the prefix.
 * @return result
 */
    public String getPrefix() {
        return switch (this) {
            case PLAYER -> "[PLAYER]: ";
            case SERVER -> "[SERVER]: ";
        };
    }
}
