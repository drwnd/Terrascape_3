package game.server.command;

import game.server.Game;

final class ClearCommand {

    static final String SYNTAX = "[Number of Messages to be spared]";
    static final String EXPLANATION = "Clears old chat Messages";

    private ClearCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        int sparedMessagesCount = 0;

        if (tokens.getNext() instanceof NumberToken number) {
            if (!number.isInteger() || number.number() < 0) return CommandResult.fail("Number must be a positiv integer");
            sparedMessagesCount = (int) number.number();
        }
        tokens.expectFinishedLess();

        Game.getServer().removeOldChatMessages(sparedMessagesCount);
        return CommandResult.success();
    }
}
