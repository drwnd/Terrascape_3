package game.server.command;

import core.settings.optionSettings.ColorOption;
import game.server.Game;

final class TimeCommand {

    static final String SYNTAX = "[set x in [-1, 1]";
    static final String EXPLANATION = "Queries the current daytime or sets it to a specified value";

    private TimeCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        if (tokens.size() == 1) {
            Game.getServer().sendServerMessage("Current Time : %s".formatted(Game.getServer().getDayTime()), ColorOption.WHITE);
            return CommandResult.success();
        }

        String keyword = tokens.expectNextKeyWord().keyword();
        if (!"set".equalsIgnoreCase(keyword)) return CommandResult.fail(keyword + " is not a valid keyword");

        float time = (float) tokens.expectNextNumber().number();
        if (Math.abs(time) > 1.0F) return CommandResult.fail("Time must be within [-1, 1]");
        tokens.expectFinishedLess();

        Game.getServer().setDayTime(time);
        return CommandResult.success();
    }
}
