package server;

import player.Player;
import rendering_api.Window;
import server.saving.PlayerSaver;
import server.saving.ServerSaver;
import server.saving.WorldSaver;

import java.io.File;

public final class Game {

    public static void play(File saveFile) {
        Material.init();
        String worldName = saveFile.getName();

        player = new PlayerSaver().load(PlayerSaver.getSaveFileLocation(worldName));
        server = new ServerSaver().load(ServerSaver.getSaveFileLocation(worldName));
        world = new WorldSaver().load(WorldSaver.getSaveFileLocation(worldName));

        world.setName(worldName);
        server.startTicks();
    }

    public static void quit() {
        Window.popRenderable();

        String worldName = world.getName();
        new PlayerSaver().save(player, PlayerSaver.getSaveFileLocation(worldName));
        new ServerSaver().save(server, ServerSaver.getSaveFileLocation(worldName));
        new WorldSaver().save(world, WorldSaver.getSaveFileLocation(worldName));

        world.cleanUp();
        player.cleanUp();
        server.cleanUp();
        Window.popRenderable();

        player = null;
        world = null;
        server = null;
    }

    public static Player getPlayer() {
        return player;
    }

    public static World getWorld() {
        return world;
    }

    public static Server getServer() {
        return server;
    }

    private static Player player;
    private static World world;
    private static Server server;
}
