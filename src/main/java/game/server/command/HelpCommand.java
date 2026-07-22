package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

final class HelpCommand {

    static final String SYNTAX = "[Command]";
    static final String EXPLANATION = "Prints Information about a Command";

    private HelpCommand() {

    }

    /**
     * Executes the help command to display information about available commands.
     * @param tokens the list of tokens from the command string
     * @return the result of the command execution
     */
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
