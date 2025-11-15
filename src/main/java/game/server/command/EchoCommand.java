package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

final class EchoCommand {

    private EchoCommand() {

    }

    static CommandResult execute(String[] tokens, String commandString) {
        Game.getServer().sendServerMessage(commandString.substring(6), ColorOption.WHITE);
        return CommandResult.success();
    }
}
