package game.server;

import core.rendering_api.Debug;
import game.player.Player;
import core.rendering_api.Window;
import game.server.material.Material;
import game.server.saving.ChunkSaver;
import game.server.saving.PlayerSaver;
import game.server.saving.ServerSaver;
import game.server.saving.WorldSaver;
import game.settings.IntSettings;

import java.io.File;

public final class Game {

/**
 * Performs play.
 *
 * @param saveFile parameter
 */
    public static void play(File saveFile) {
        Material.loadMaterials();
        String worldName = saveFile.getName();

        world = new WorldSaver().load(WorldSaver.getSaveFileLocation(worldName));
        server = new ServerSaver().load(ServerSaver.getSaveFileLocation(worldName));
        player = new PlayerSaver().load(PlayerSaver.getSaveFileLocation(worldName));

        world.setName(worldName);
        World.init();
        server.startTicks();
        Window.setCrashCallback(server);
    }

/**
 * Performs quit.
 */
    public static void quit() {
        Window.popRenderable();
        cleanUp();
        Window.popRenderable();

        player = null;
        world = null;
        server = null;
    }

/**
 * Performs clean up.
 */
    public static void cleanUp() {
        if (world == null) return;
        String worldName = world.getName();
        new PlayerSaver().save(player, PlayerSaver.getSaveFileLocation(worldName));
        new ServerSaver().save(server, ServerSaver.getSaveFileLocation(worldName));
        new WorldSaver().save(world, WorldSaver.getSaveFileLocation(worldName));

        world.cleanUp();
        player.cleanUp();
        server.cleanUp();
    }

/**
 * Performs update render distance.
 *
 * @param oldRenderDistance parameter
 */
    public static void updateRenderDistance(int oldRenderDistance) {
        if (world == null || oldRenderDistance == IntSettings.RENDER_DISTANCE.value()) return;
        server = new Server(server);
        world = new World(world, true);
        player.updateRenderDistance(oldRenderDistance);

        server.startTicks();
    }

/**
 * Performs update lod count.
 *
 * @param oldLodCount parameter
 */
    public static void updateLodCount(int oldLodCount) {
        if (world == null || oldLodCount == IntSettings.LOD_COUNT.value()) return;
        server = new Server(server);
        world = new World(world, false);
        player.updateLodCount();

        if (oldLodCount < IntSettings.LOD_COUNT.value()) ChunkSaver.generateHigherLODs();
        else World.deleteHigherLODs(IntSettings.LOD_COUNT.value());

        server.startTicks();
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


/**
 * Sets temporary world.
 *
 * @param world parameter
 * @return true if the condition holds
 */
    public static boolean setTemporaryWorld(World world) {
        if (Game.world != null || player != null || server != null) {
            Debug.err("Cannot set temporary World. The Game might be running");
            return false;
        }
        Game.world = world;
        return true;
    }

/**
 * Removes temporary world.
 */
    public static void removeTemporaryWorld() {
        if (player != null || server != null) {
            Debug.err("Cannot remove temporary World. The Game might be running");
            return;
        }
        world = null;
    }


    private static Player player;
    private static World world;
    private static Server server;
}
