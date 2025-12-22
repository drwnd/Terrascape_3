package game.server;

import game.player.Player;
import core.rendering_api.Window;
import game.server.material.Material;
import game.server.saving.PlayerSaver;
import game.server.saving.ServerSaver;
import game.server.saving.WorldSaver;

import java.io.File;

public final class Game {

    public static void play(File saveFile) {
        Material.loadMaterials();
        String worldName = saveFile.getName();

        player = new PlayerSaver().load(PlayerSaver.getSaveFileLocation(worldName));
        server = new ServerSaver().load(ServerSaver.getSaveFileLocation(worldName));
        world = new WorldSaver().load(WorldSaver.getSaveFileLocation(worldName));

        world.setName(worldName);
        World.init();
        server.startTicks();
        Window.setCrashCallback(server);
    }

    public static void quit() {
        Window.popRenderable();
        cleanUp();
        Window.popRenderable();

        player = null;
        world = null;
        server = null;
    }

    public static void cleanUp() {
        String worldName = world.getName();
        new PlayerSaver().save(player, PlayerSaver.getSaveFileLocation(worldName));
        new ServerSaver().save(server, ServerSaver.getSaveFileLocation(worldName));
        new WorldSaver().save(world, WorldSaver.getSaveFileLocation(worldName));

        world.cleanUp();
        player.cleanUp();
        server.cleanUp();
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


    public static boolean setTemporaryWorld(World world) {
        if (Game.world != null || player != null || server != null) {
            System.err.println("Cannot set temporary World. The Game might be running");
            return false;
        }
        Game.world = world;
        return true;
    }

    public static void removeTemporaryWorld() {
        if (player != null || server != null) {
            System.err.println("Cannot remove temporary World. The Game might be running");
            return;
        }
        world = null;
    }


    private static Player player;
    private static World world;
    private static Server server;
}
