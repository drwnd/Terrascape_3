package game.server.generation;

import game.player.rendering.MeshCollector;
import game.player.rendering.MeshGenerator;
import game.server.*;
import game.server.saving.ChunkSaver;
import game.utils.Status;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static game.utils.Constants.*;

public final class ChunkGenerator {

    public ChunkGenerator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    public static void loadImmediateSurroundings() {
        Vector3i playerPosition = Game.getPlayer().getPosition().intPosition();

        int playerChunkX = playerPosition.x >> CHUNK_SIZE_BITS;
        int playerChunkY = playerPosition.y >> CHUNK_SIZE_BITS;
        int playerChunkZ = playerPosition.z >> CHUNK_SIZE_BITS;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
        executor.submit(new Generator(playerChunkX + 0, playerChunkY, playerChunkZ + 1, 0));
        executor.submit(new Generator(playerChunkX + 0, playerChunkY, playerChunkZ + 0, 0));
        executor.submit(new Generator(playerChunkX + 0, playerChunkY, playerChunkZ - 1, 0));
        executor.submit(new Generator(playerChunkX + 1, playerChunkY, playerChunkZ + 1, 0));
        executor.submit(new Generator(playerChunkX + 1, playerChunkY, playerChunkZ + 0, 0));
        executor.submit(new Generator(playerChunkX + 1, playerChunkY, playerChunkZ - 1, 0));
        executor.submit(new Generator(playerChunkX - 1, playerChunkY, playerChunkZ + 1, 0));
        executor.submit(new Generator(playerChunkX - 1, playerChunkY, playerChunkZ + 0, 0));
        executor.submit(new Generator(playerChunkX - 1, playerChunkY, playerChunkZ - 1, 0));

        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            System.err.println("Executor failed to generate immediate surroundings.");
        }
    }

    public void restart() {
        Vector3i playerChunkPosition = Game.getPlayer().getPosition().getChunkCoordinate();
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

    private void submitTasks(int playerChunkX, int playerChunkY, int playerChunkZ) {
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            int lodPlayerX = playerChunkX >> lod;
            int lodPlayerY = playerChunkY >> lod;
            int lodPlayerZ = playerChunkZ >> lod;

            for (int ring = 0; ring <= RENDER_DISTANCE_XZ + 1; ring++) {
                submitRingGeneration(lodPlayerX, lodPlayerY, lodPlayerZ, ring, lod);
                submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, ring - 2, lod);
            }
            submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, RENDER_DISTANCE_XZ, lod);
        }
    }

    private void submitRingMeshing(int playerChunkX, int playerChunkY, int playerChunkZ, int ring, int lod) {
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

    private void submitRingGeneration(int playerChunkX, int playerChunkY, int playerChunkZ, int ring, int lod) {
        if (ring == 0) {
            submitColumnGeneration(playerChunkX, playerChunkY, playerChunkZ, lod);
            return;
        }

        for (int chunkX = -ring; chunkX < ring; chunkX++) submitColumnGeneration(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod);
        for (int chunkZ = ring; chunkZ > -ring; chunkZ--) submitColumnGeneration(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
        for (int chunkX = ring; chunkX > -ring; chunkX--) submitColumnGeneration(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod);
        for (int chunkZ = -ring; chunkZ < ring; chunkZ++) submitColumnGeneration(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
    }

    private void submitColumnGeneration(int chunkX, int playerChunkY, int chunkZ, int lod) {
        if (executor.isShutdown()) return;
        if (columnRequiresGeneration(chunkX, playerChunkY, chunkZ, lod))
            executor.submit(new Generator(chunkX, playerChunkY, chunkZ, lod));
    }

    private void submitColumnMeshing(int chunkX, int playerChunkY, int chunkZ, int lod) {
        if (executor.isShutdown()) return;
        if (columnRequiresMeshing(chunkX, playerChunkY, chunkZ, lod))
            executor.submit(new MeshHandler(chunkX, playerChunkY, chunkZ, lod));
    }

    private static boolean columnRequiresGeneration(int chunkX, int playerChunkY, int chunkZ, int lod) {
        World world = Game.getWorld();
        for (int chunkY = playerChunkY - RENDER_DISTANCE_Y - 1; chunkY < playerChunkY + RENDER_DISTANCE_Y + 2; chunkY++)
            if (world.getGenerationStatus(chunkX, chunkY, chunkZ, lod) == Status.NOT_STARTED) return true;
        return false;
    }

    private static boolean columnRequiresMeshing(int chunkX, int playerChunkY, int chunkZ, int lod) {
        World world = Game.getWorld();
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        for (int chunkY = playerChunkY - RENDER_DISTANCE_Y; chunkY < playerChunkY + RENDER_DISTANCE_Y + 1; chunkY++) {
            int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
            Chunk chunk = world.getChunk(chunkIndex, lod);
            if (chunk == null || !meshCollector.isMeshed(chunkIndex, lod)) return true;
        }
        return false;
    }


    private final ThreadPoolExecutor executor;

    private record Generator(int chunkX, int playerChunkY, int chunkZ, int lod) implements Runnable {

        @Override
        public void run() {

            GenerationData generationData = new GenerationData(chunkX, chunkZ, lod);
            ChunkSaver saver = new ChunkSaver();

            for (int chunkY = playerChunkY - RENDER_DISTANCE_Y - 1; chunkY < playerChunkY + RENDER_DISTANCE_Y + 2; chunkY++) {
                try {
                    Chunk chunk = saver.load(chunkX, chunkY, chunkZ, lod);
                    if (chunk.getGenerationStatus() == Status.NOT_STARTED) {
                        WorldGeneration.generate(chunk, generationData);
                        Game.getWorld().storeChunk(chunk);
                    }
                } catch (Exception exception) {
                    System.err.println("Generation:");
                    System.err.println(exception.getClass());
                    exception.printStackTrace();
                    System.err.printf("%d %d %d%n", chunkX, chunkY, chunkZ);
                }
            }
        }
    }

    private record MeshHandler(int chunkX, int playerChunkY, int chunkZ, int lod) implements Runnable {

        @Override
        public void run() {

            MeshGenerator meshGenerator = new MeshGenerator();
            World world = Game.getWorld();
            MeshCollector meshCollector = Game.getPlayer().getMeshCollector();

            for (int chunkY = playerChunkY - RENDER_DISTANCE_Y; chunkY < playerChunkY + RENDER_DISTANCE_Y + 1; chunkY++) {
                try {
                    int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
                    long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ, lod);
                    Chunk chunk = world.getChunk(chunkIndex, lod);
                    if (chunk == null) {
                        System.err.println("to mesh chunk is null" + chunkX + " " + chunkY + " " + chunkZ + " " + lod);
                        continue;
                    }
                    if (chunk.ID != expectedId) {
                        System.err.println("Chunk has wrong ID" + chunkX + " " + chunkY + " " + chunkZ + " " + lod + " is " + chunk.ID + " should be " + expectedId);
                        continue;
                    }
                    if (chunk.getGenerationStatus() != Status.DONE) {
                        System.err.println("to mesh chunk hasn't been generated " + chunk.getGenerationStatus().name());
                        System.err.println(chunkX + " " + chunkY + " " + chunkZ + " " + lod);
                        continue;
                    }
                    if (meshCollector.isMeshed(chunkIndex, lod)) continue;
                    meshGenerator.generateMesh(chunk);

                } catch (Exception exception) {
                    System.err.println("Meshing:");
                    System.err.println(exception.getClass());
                    exception.printStackTrace();
                    System.err.printf("%d %d %d%n", chunkX, chunkY, chunkZ);
                }
            }
        }
    }
}
