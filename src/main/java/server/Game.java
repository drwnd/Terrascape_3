package server;

import player.Player;
import rendering_api.Window;

import java.io.File;

public final class Game {

    public static void play(File saveFile) {
        FileManager.loadUniversalFiles(saveFile);
        Material.init();

        player = FileManager.loadPlayer();
        server = FileManager.loadServer();
        world = new World(saveFile.getName());

        server.startTicks();
    }

    public static void quit() {
        Window.popRenderable();
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
