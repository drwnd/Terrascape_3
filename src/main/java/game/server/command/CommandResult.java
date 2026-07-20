package game.server.command;

/**
 * Performs command result.
 *
 * @param successful parameter
 * @param reason parameter
 * @return result
 */
public record CommandResult(boolean successful, String reason) {

    public static CommandResult success() {
        return new CommandResult(true, "");
    }

    public static CommandResult fail(String reason) {
        return new CommandResult(false, reason);
    }
}
