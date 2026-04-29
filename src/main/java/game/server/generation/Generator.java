package game.server.generation;

import core.utils.Vector3l;

import game.player.rendering.MeshCollector;
import game.server.*;
import game.utils.Status;
import game.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static game.utils.Constants.*;

public final class Generator {

    public Generator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    public static void loadImmediateSurroundings() {
        Vector3l playerPosition = Game.getPlayer().getPosition().longPosition();

        long playerChunkX = playerPosition.x >>> CHUNK_SIZE_BITS;
        long playerChunkY = playerPosition.y >>> CHUNK_SIZE_BITS;
        long playerChunkZ = playerPosition.z >>> CHUNK_SIZE_BITS;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
        executor.submit(getChunkGenerator(playerChunkX + 0, playerChunkY, playerChunkZ + 1, 0));
        executor.submit(getChunkGenerator(playerChunkX + 0, playerChunkY, playerChunkZ + 0, 0));
        executor.submit(getChunkGenerator(playerChunkX + 0, playerChunkY, playerChunkZ - 1, 0));
        executor.submit(getChunkGenerator(playerChunkX + 1, playerChunkY, playerChunkZ + 1, 0));
        executor.submit(getChunkGenerator(playerChunkX + 1, playerChunkY, playerChunkZ + 0, 0));
        executor.submit(getChunkGenerator(playerChunkX + 1, playerChunkY, playerChunkZ - 1, 0));
        executor.submit(getChunkGenerator(playerChunkX - 1, playerChunkY, playerChunkZ + 1, 0));
        executor.submit(getChunkGenerator(playerChunkX - 1, playerChunkY, playerChunkZ + 0, 0));
        executor.submit(getChunkGenerator(playerChunkX - 1, playerChunkY, playerChunkZ - 1, 0));

        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            System.err.println("Executor failed to generate immediate surroundings.");
        }
    }

    public void restart() {
        Vector3l playerChunkPosition = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (this) {
            executor.getQueue().clear();
        }
        Server.unloadDistantChunks(playerChunkPosition);

        submitTasks(playerChunkPosition.x, playerChunkPosition.y, playerChunkPosition.z);
    }

    public void cleanUp() {
        waitUntilHalt();
    }


    private void waitUntilHalt() {
        synchronized (this) {
            executor.getQueue().clear();
        }
        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.err.println("Crashed when awaiting termination");
            e.printStackTrace();
        }
    }

    private void submitTasks(long playerChunkX, long playerChunkY, long playerChunkZ) {
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            long lodPlayerX = playerChunkX >> lod;
            long lodPlayerY = playerChunkY >> lod;
            long lodPlayerZ = playerChunkZ >> lod;

            for (int ring = 0; ring <= RENDER_DISTANCE + 1; ring++) {
                submitRingGeneration(lodPlayerX, lodPlayerY, lodPlayerZ, ring, lod);
                submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, ring - 2, lod);
            }
            submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, RENDER_DISTANCE, lod);
        }
    }

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

    private void submitColumnGeneration(long chunkX, long playerChunkY, long chunkZ, int lod) {
        if (executor.isShutdown()) return;
        if (columnRequiresGeneration(chunkX, playerChunkY, chunkZ, lod))
            executor.submit(getChunkGenerator(chunkX, playerChunkY, chunkZ, lod));
    }

    private void submitColumnMeshing(long chunkX, long playerChunkY, long chunkZ, int lod) {
        if (executor.isShutdown()) return;
        if (columnRequiresMeshing(chunkX, playerChunkY, chunkZ, lod))
            executor.submit(getMeshGenerator(chunkX, playerChunkY, chunkZ, lod));
    }


    private static boolean columnRequiresGeneration(long chunkX, long playerChunkY, long chunkZ, int lod) {
        World world = Game.getWorld();
        for (long chunkY = playerChunkY - RENDER_DISTANCE - 1; chunkY != playerChunkY + RENDER_DISTANCE + 2; chunkY++)
            if (world.getGenerationStatus(chunkX, chunkY, chunkZ, lod) == Status.NOT_STARTED) return true;
        return false;
    }

    private static boolean columnRequiresMeshing(long chunkX, long playerChunkY, long chunkZ, int lod) {
        World world = Game.getWorld();
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        for (long chunkY = playerChunkY - RENDER_DISTANCE; chunkY != playerChunkY + RENDER_DISTANCE + 1; chunkY++) {
            int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
            Chunk chunk = world.getChunk(chunkIndex, lod);
            if (chunk == null || !meshCollector.isMeshed(chunkIndex, lod)) return true;
        }
        return false;
    }

    private static Runnable getChunkGenerator(long chunkX, long playerChunkY, long chunkZ, int lod) {
        return new JavaChunkGenerator(chunkX, playerChunkY, chunkZ, lod);
    }

    private static Runnable getMeshGenerator(long chunkX, long playerChunkY, long chunkZ, int lod) {
        return new JavaMeshGenerator(chunkX, playerChunkY, chunkZ, lod);
    }

    private final ThreadPoolExecutor executor;
}
