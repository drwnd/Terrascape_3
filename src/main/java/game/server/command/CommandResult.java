package game.server.command;

public record CommandResult(boolean successful, String reason) {

    public static CommandResult success() {
        return new CommandResult(true, "");
    }

    public static CommandResult fail(String reason) {
        return new CommandResult(false, reason);
    }
}
