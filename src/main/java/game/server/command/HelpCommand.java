package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

import java.util.ArrayList;

final class HelpCommand {

    static final String SYNTAX = "[Command]";
    static final String EXPLANATION = "Prints Information about a Command";

    private HelpCommand() {

    }

    static CommandResult execute(ArrayList<Token> tokens) {
        if (tokens.size() == 1) {
            for (Command command : Command.values()) Game.getServer().sendServerMessage(command.name(), ColorOption.WHITE);
            return CommandResult.success();
        }

        if (Token.isKeyWord(1, tokens)) {
            String commandName = ((KeyWordToken) tokens.get(1)).keyword().toUpperCase();
            Command command = Command.valueOf(commandName);

            Game.getServer().sendServerMessage(command.getExplanation(), ColorOption.WHITE);
            Game.getServer().sendServerMessage(command.getSyntax(), ColorOption.WHITE);
            return CommandResult.success();
        }

        return CommandResult.fail("Unexpected tokens");
    }
}
