package server;

import org.joml.Vector3i;
import player.rendering.MeshCollector;
import server.generation.ChunkGenerator;
import server.saving.ChunkSaver;
import utils.Position;
import utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static utils.Constants.*;

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


    public boolean requestBreakPlaceInteraction(Vector3i position, int breakPlaceSize, byte material) {
        int x = position.x;
        int y = position.y;
        int z = position.z;

        Chunk chunk = Game.getWorld().getChunk(x >> CHUNK_SIZE_BITS, y >> CHUNK_SIZE_BITS, z >> CHUNK_SIZE_BITS, 0);
        if (chunk == null) return false;

        x &= -breakPlaceSize & CHUNK_SIZE_MASK;
        y &= -breakPlaceSize & CHUNK_SIZE_MASK;
        z &= -breakPlaceSize & CHUNK_SIZE_MASK;

        chunk.storeMaterial(x, y, z, material, breakPlaceSize);

        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        meshCollector.setMeshed(false, chunk.INDEX, chunk.LOD);
        if (x == 0) meshCollector.setMeshed(false, chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD);
        if (y == 0) meshCollector.setMeshed(false, chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD);
        if (z == 0) meshCollector.setMeshed(false, chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD);

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
