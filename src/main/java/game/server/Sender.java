package game.server;

public enum Sender {
    PLAYER,
    SERVER,
    CONTINUE;

    public String getPrefix() {
        return switch (this) {
            case PLAYER -> "[PLAYER]: ";
            case SERVER -> "[SERVER]: ";
            case CONTINUE -> "          ";
        };
    }
}
