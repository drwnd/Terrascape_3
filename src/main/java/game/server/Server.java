package game.server;

import game.player.interaction.Placeable;
import org.joml.Vector3i;
import game.player.rendering.MeshCollector;
import game.server.generation.ChunkGenerator;
import game.server.saving.ChunkSaver;
import game.utils.Position;
import game.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static game.utils.Constants.*;

public final class Server {

    public Server(long currentGameTick) {
        this.currentGameTick = currentGameTick;
    }

    public float getCurrentGameTickFraction() {
        long currentGameTickDuration = System.nanoTime() - gameTickStartTime;
        return (float) ((double) currentGameTickDuration / NANOSECONDS_PER_GAME_TICK);
    }

    public long getCurrentGameTick() {
        return currentGameTick;
    }


    public boolean requestBreakPlaceInteraction(Vector3i position, Placeable placeable) {
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            placeable.place(position, lod);
            for (Chunk chunk : placeable.getAffectedChunks()) {
                if (chunk == null) continue;
                meshCollector.setMeshed(false, chunk.INDEX, chunk.LOD);
            }
        }
        generatorRestartScheduled = true;
        return true;
    }

    public void pauseTicks() {
        if (executor != null) executor.shutdownNow();
    }

    public void startTicks() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::executeGameTickCatchException, 0, NANOSECONDS_PER_GAME_TICK, TimeUnit.NANOSECONDS);
    }

    void cleanUp() {
        generator.cleanUp();
    }

    public void unloadDistantChunks(Vector3i playerChunkPosition) {
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        ChunkSaver saver = new ChunkSaver();

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            int lodPlayerX = playerChunkPosition.x >> lod;
            int lodPlayerY = playerChunkPosition.y >> lod;
            int lodPlayerZ = playerChunkPosition.z >> lod;

            for (Chunk chunk : Game.getWorld().getLod(lod)) {
                if (chunk == null) continue;

                if (Utils.outsideRenderKeepDistance(lodPlayerX, lodPlayerY, lodPlayerZ, chunk.X, chunk.Y, chunk.Z))
                    meshCollector.removeMesh(chunk.INDEX, chunk.LOD);

                if (Utils.outsideChunkKeepDistance(lodPlayerX, lodPlayerY, lodPlayerZ, chunk.X, chunk.Y, chunk.Z)) {
                    Game.getWorld().setNull(chunk.INDEX, chunk.LOD);
                    if (chunk.isModified()) saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
                }
            }
        }
    }


    private void executeGameTickCatchException() {
        try {
            gameTickStartTime = System.nanoTime();
            executeGameTick();
            currentGameTick++;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void executeGameTick() {
        Position oldPlayerPosition = Game.getPlayer().getPosition();
        Game.getPlayer().updateGameTick();
        Position newPlayerPosition = Game.getPlayer().getPosition();
        if (!oldPlayerPosition.sharesChunkWith(newPlayerPosition) || generatorRestartScheduled) {
            generator.restart();
            generatorRestartScheduled = false;
        }
    }

    private ScheduledExecutorService executor;
    private final ChunkGenerator generator = new ChunkGenerator();
    private long gameTickStartTime;
    private long currentGameTick;
    private boolean generatorRestartScheduled = true;

    private static final int NANOSECONDS_PER_GAME_TICK = 50_000_000;
}
