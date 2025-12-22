package game.server.command;

final class CrashCommand {

    static final String SYNTAX = "";
    static final String EXPLANATION = "Crashes the Game.";

    private CrashCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        throw new CrashException();
    }
}
