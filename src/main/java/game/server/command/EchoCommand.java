package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

import java.awt.*;
import java.util.ArrayList;

final class EchoCommand {

    static final String SYNTAX = "[Number of Repeats] [Color of Messages] \"Message\"";
    static final String EXPLANATION = "Prints a specified Message";

    private EchoCommand() {

    }

    static CommandResult execute(ArrayList<Token> tokens) {
        ColorOption color = null;
        String message = null;
        int printCount = 1;
        int tokenIndex = 1;

        if (Token.isNumber(tokenIndex, tokens)) {
            NumberToken token = (NumberToken) tokens.get(tokenIndex);
            if (!token.isInteger() || token.number() < 0) return CommandResult.fail("Number must be a positiv integer");
            printCount = (int) token.number();
            tokenIndex++;
        }
        if (Token.isKeyWord(tokenIndex, tokens)) {
            color = ColorOption.valueOf(((KeyWordToken) tokens.get(tokenIndex)).keyword().toUpperCase());
            tokenIndex++;
        }
        if (Token.isString(tokenIndex, tokens)) {
            message = ((StringToken) tokens.get(tokenIndex)).string();
        }
        if (message == null) return CommandResult.fail("No message found");
        if (color == null) color = ColorOption.WHITE;

        for (int count = 0; count < printCount; count++) Game.getServer().sendServerMessage(message, color);
        return CommandResult.success();
    }
}
