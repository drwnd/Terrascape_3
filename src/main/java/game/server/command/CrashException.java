package game.server.command;

import core.utils.MainThread;

public class CrashException extends RuntimeException {

    @MainThread
    public CrashException() {
        super("You did this to yourself.");
    }
}
