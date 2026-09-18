package game.server.command;

import core.utils.MainThread;

public record CommandResult(boolean successful, String reason) {

    @MainThread
    public static CommandResult success() {
        return new CommandResult(true, "");
    }

    @MainThread
    public static CommandResult fail(String reason) {
        return new CommandResult(false, reason);
    }
}
