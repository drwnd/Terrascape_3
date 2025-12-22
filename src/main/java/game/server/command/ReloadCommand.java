package game.server.command;

import game.server.Game;
import game.server.Server;

final class ReloadCommand {

    static final String SYNTAX = "{Mesh, Chunk}";
    static final String EXPLANATION = "Regenerates all Meshes or all Chunks and Meshes";

    private ReloadCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        String keyword = tokens.expectNextKeyWord().keyword();
        tokens.expectFinishedLess();

        if ("mesh".equalsIgnoreCase(keyword)) Game.getPlayer().getMeshCollector().removeAll();
        else if ("chunk".equalsIgnoreCase(keyword)) {
            Server.unloadAll();
            Game.getPlayer().getMeshCollector().removeAll();
        } else return CommandResult.fail("Unknown target : " + keyword);

        Game.getServer().scheduleGeneratorRestart();
        return CommandResult.success();
    }
}
