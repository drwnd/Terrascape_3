package game.server.command;

import game.server.Game;

import java.util.ArrayList;

final class ClearCommand {

    static final String SYNTAX = "[Number of Messages to be spared]";
    static final String EXPLANATION = "Clears old chat Messages";

    private ClearCommand() {

    }

    static CommandResult execute(ArrayList<Token> tokens) {
        int sparedMessagesCount = 0;

        if (Token.isNumber(1, tokens)) {
            NumberToken token = (NumberToken) tokens.get(1);
            if (!token.isInteger() || token.number() < 0) return CommandResult.fail("Number must be a positiv integer");
            sparedMessagesCount = (int) token.number();
        }

        Game.getServer().removeOldChatMessages(sparedMessagesCount);
        return CommandResult.success();
    }
}
