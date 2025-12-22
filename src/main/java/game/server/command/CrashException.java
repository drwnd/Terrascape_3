package game.server.command;

public class CrashException extends RuntimeException {
    public CrashException() {
        super("You did this to yourself.");
    }
}
