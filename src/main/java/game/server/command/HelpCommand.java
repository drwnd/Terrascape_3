package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

final class HelpCommand {

    static final String SYNTAX = "[Command]";
    static final String EXPLANATION = "Prints Information about a Command";

    private HelpCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        if (tokens.size() == 1) {
            for (Command command : Command.values()) Game.getServer().sendServerMessage(command.name(), ColorOption.WHITE);
            return CommandResult.success();
        }

        String commandName = tokens.expectNextKeyWord().keyword().toUpperCase();
        Command command = Command.getCommand(commandName);

        tokens.expectFinishedLess();
        Game.getServer().sendServerMessage(command.getExplanation(), ColorOption.WHITE);
        Game.getServer().sendServerMessage(command.getSyntax(), ColorOption.WHITE);
        return CommandResult.success();
    }
}
