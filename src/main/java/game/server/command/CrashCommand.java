package game.server.command;

import core.utils.MainThread;

final class CrashCommand {

    static final String SYNTAX = "";
    static final String EXPLANATION = "Crashes the Game.";

    private CrashCommand() {

    }

    @MainThread
    static CommandResult execute(TokenList tokens) {
        throw new CrashException();
    }
}
