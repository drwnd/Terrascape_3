package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

final class EchoCommand {

    static final String SYNTAX = "[Number of Repeats] [Color of Messages] \"Message\"";
    static final String EXPLANATION = "Prints a specified Message";

    private EchoCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        ColorOption color = null;
        int printCount = 1;

        if (tokens.nextIncrementNumber() instanceof NumberToken number) {
            if (!number.isInteger() || number.number() < 0) return CommandResult.fail("Number must be a positiv integer");
            printCount = (int) number.number();
        }
        if (tokens.getIncrementKeyword() instanceof KeywordToken(String keyword)) color = ColorOption.valueOf(keyword.toUpperCase());
        String message = tokens.expectGetString().string();

        tokens.expectFinishedLess();
        if (color == null) color = ColorOption.WHITE;

        for (int count = 0; count < printCount; count++) Game.getServer().sendServerMessage(message, color);
        return CommandResult.success();
    }
}
