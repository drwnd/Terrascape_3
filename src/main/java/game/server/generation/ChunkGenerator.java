package game.server.generation;

import core.rendering_api.Debug;
import core.utils.Vector3l;

import game.player.rendering.MeshCollector;
import game.server.*;
import game.settings.IntSettings;
import core.utils.MainThread;
import game.utils.ServerThread;
import game.utils.Status;
import game.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static game.utils.Constants.*;

public final class ChunkGenerator {

    @MainThread
    public ChunkGenerator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    @MainThread
    public static void loadImmediateSurroundings() {
        Vector3l playerPosition = Game.getPlayer().getPosition().longPosition();

        long playerChunkX = playerPosition.x >>> CHUNK_SIZE_BITS;
        long playerChunkY = playerPosition.y >>> CHUNK_SIZE_BITS;
        long playerChunkZ = playerPosition.z >>> CHUNK_SIZE_BITS;

        try (ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS)) {
            executor.submit(new ChunkColumnGenerator(playerChunkX + 0, playerChunkY, playerChunkZ + 1, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX + 0, playerChunkY, playerChunkZ + 0, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX + 0, playerChunkY, playerChunkZ - 1, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX + 1, playerChunkY, playerChunkZ + 1, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX + 1, playerChunkY, playerChunkZ + 0, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX + 1, playerChunkY, playerChunkZ - 1, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX - 1, playerChunkY, playerChunkZ + 1, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX - 1, playerChunkY, playerChunkZ + 0, 0));
            executor.submit(new ChunkColumnGenerator(playerChunkX - 1, playerChunkY, playerChunkZ - 1, 0));

            executor.shutdown();

            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            Debug.err("Executor failed to generate immediate surroundings.");
        }
    }

    @ServerThread
    public void restart() {
        Vector3l playerChunkPosition = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (this) {
            executor.getQueue().clear();
        }
        Server.unloadDistantChunks(playerChunkPosition);

        submitTasks(playerChunkPosition.x, playerChunkPosition.y, playerChunkPosition.z);
    }

    @MainThread
    public void cleanUp() {
        waitUntilHalt();
    }

    @MainThread
    private void waitUntilHalt() {
        synchronized (this) {
            executor.getQueue().clear();
        }
        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException exception) {
            Debug.err("Crashed when awaiting termination");
            Debug.err(exception);
        }
    }

    @ServerThread
    private void submitTasks(long playerChunkX, long playerChunkY, long playerChunkZ) {
        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            long lodPlayerX = playerChunkX >> lod;
            long lodPlayerY = playerChunkY >> lod;
            long lodPlayerZ = playerChunkZ >> lod;

            for (int ring = 0; ring <= IntSettings.RENDER_DISTANCE.value() + 1; ring++) {
                submitRingGeneration(lodPlayerX, lodPlayerY, lodPlayerZ, ring, lod);
                submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, ring - 2, lod);
            }
            submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, IntSettings.RENDER_DISTANCE.value(), lod);
        }
    }

    @ServerThread
    private void submitRingMeshing(long playerChunkX, long playerChunkY, long playerChunkZ, int ring, int lod) {
        if (ring < 0) return;
        if (ring == 0) {
            submitColumnMeshing(playerChunkX, playerChunkY, playerChunkZ, lod);
            return;
        }

        for (int chunkX = -ring; chunkX < ring; chunkX++) submitColumnMeshing(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod);
        for (int chunkZ = ring; chunkZ > -ring; chunkZ--) submitColumnMeshing(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
        for (int chunkX = ring; chunkX > -ring; chunkX--) submitColumnMeshing(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod);
        for (int chunkZ = -ring; chunkZ < ring; chunkZ++) submitColumnMeshing(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
    }

    @ServerThread
    private void submitRingGeneration(long playerChunkX, long playerChunkY, long playerChunkZ, int ring, int lod) {
        if (ring == 0) {
            submitColumnGeneration(playerChunkX, playerChunkY, playerChunkZ, lod);
            return;
        }

        for (int chunkX = -ring; chunkX < ring; chunkX++) submitColumnGeneration(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod);
        for (int chunkZ = ring; chunkZ > -ring; chunkZ--) submitColumnGeneration(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
        for (int chunkX = ring; chunkX > -ring; chunkX--) submitColumnGeneration(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod);
        for (int chunkZ = -ring; chunkZ < ring; chunkZ++) submitColumnGeneration(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
    }

    @ServerThread
    private void submitColumnGeneration(long chunkX, long playerChunkY, long chunkZ, int lod) {
        if (executor.isShutdown()) return;
        if (columnRequiresGeneration(chunkX, playerChunkY, chunkZ, lod))
            executor.submit(new ChunkColumnGenerator(chunkX, playerChunkY, chunkZ, lod));
    }

    @ServerThread
    private void submitColumnMeshing(long chunkX, long playerChunkY, long chunkZ, int lod) {
        if (executor.isShutdown()) return;
        if (columnRequiresMeshing(chunkX, playerChunkY, chunkZ, lod))
            executor.submit(new ChunkColumnMesher(chunkX, playerChunkY, chunkZ, lod));
    }

    @ServerThread
    private static boolean columnRequiresGeneration(long chunkX, long playerChunkY, long chunkZ, int lod) {
        World world = Game.getWorld();
        for (long chunkY = playerChunkY - IntSettings.RENDER_DISTANCE.value() - 1; chunkY != playerChunkY + IntSettings.RENDER_DISTANCE.value() + 2; chunkY++)
            if (world.getGenerationStatus(chunkX, chunkY, chunkZ, lod) == Status.NOT_STARTED) return true;
        return false;
    }

    @ServerThread
    private static boolean columnRequiresMeshing(long chunkX, long playerChunkY, long chunkZ, int lod) {
        World world = Game.getWorld();
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        for (long chunkY = playerChunkY - IntSettings.RENDER_DISTANCE.value(); chunkY != playerChunkY + IntSettings.RENDER_DISTANCE.value() + 1; chunkY++) {
            int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
            Chunk chunk = world.getChunk(chunkIndex, lod);
            if (chunk == null || !meshCollector.isMeshed(chunkIndex, lod)) return true;
        }
        return false;
    }


    private final ThreadPoolExecutor executor;
}
