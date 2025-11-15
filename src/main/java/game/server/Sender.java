package game.server;

public enum Sender {
    PLAYER,
    SERVER;

    public String getPrefix() {
        return switch (this) {
            case PLAYER -> "[PLAYER]: ";
            case SERVER -> "[SERVER]: ";
        };
    }
}
