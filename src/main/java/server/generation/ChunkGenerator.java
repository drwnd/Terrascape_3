package server.generation;

import org.joml.Vector3i;
import player.Player;
import player.rendering.MeshCollector;
import player.rendering.MeshGenerator;
import server.*;
import utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static utils.Constants.*;

public final class ChunkGenerator {

    public ChunkGenerator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    public void restart() {
        Vector3i playerChunkPosition = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (executor) {
            executor.getQueue().clear();
        }
        executor.getQueue().clear();
        Game.getServer().unloadDistantChunks(playerChunkPosition);

        submitTasks(playerChunkPosition.x, playerChunkPosition.y, playerChunkPosition.z);
    }

    public void cleanUp() {
        waitUntilHalt();
    }


    private void waitUntilHalt() {
        executor.getQueue().clear();
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
            if (columnRequiresMeshing(playerChunkX, playerChunkY, playerChunkZ, lod))
                executor.submit(new MeshHandler(playerChunkX, playerChunkY, playerChunkZ, lod));
            return;
        }

        for (int chunkX = -ring; chunkX < ring && !executor.isShutdown(); chunkX++)
            if (columnRequiresMeshing(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod))
                executor.submit(new MeshHandler(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod));

        for (int chunkZ = ring; chunkZ > -ring && !executor.isShutdown(); chunkZ--)
            if (columnRequiresMeshing(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new MeshHandler(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod));

        for (int chunkX = ring; chunkX > -ring && !executor.isShutdown(); chunkX--)
            if (columnRequiresMeshing(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod))
                executor.submit(new MeshHandler(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod));

        for (int chunkZ = -ring; chunkZ < ring && !executor.isShutdown(); chunkZ++)
            if (columnRequiresMeshing(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new MeshHandler(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod));
    }

    private void submitRingGeneration(int playerChunkX, int playerChunkY, int playerChunkZ, int ring, int lod) {
        if (ring == 0) {
            if (!executor.isShutdown() && columnRequiresGeneration(playerChunkX, playerChunkY, playerChunkZ, lod))
                executor.submit(new Generator(playerChunkX, playerChunkY, playerChunkZ, lod));
            return;
        }

        for (int chunkX = -ring; chunkX < ring && !executor.isShutdown(); chunkX++)
            if (columnRequiresGeneration(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod))
                executor.submit(new Generator(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod));

        for (int chunkZ = ring; chunkZ > -ring && !executor.isShutdown(); chunkZ--)
            if (columnRequiresGeneration(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new Generator(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod));

        for (int chunkX = ring; chunkX > -ring && !executor.isShutdown(); chunkX--)
            if (columnRequiresGeneration(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod))
                executor.submit(new Generator(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod));

        for (int chunkZ = -ring; chunkZ < ring && !executor.isShutdown(); chunkZ++)
            if (columnRequiresGeneration(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new Generator(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod));
    }

    private boolean columnRequiresGeneration(int chunkX, int playerChunkY, int chunkZ, int lod) {
        for (int chunkY = playerChunkY - RENDER_DISTANCE_Y - 1; chunkY < playerChunkY + RENDER_DISTANCE_Y + 2; chunkY++) {
            Chunk chunk = Game.getWorld().getChunk(chunkX, chunkY, chunkZ, lod);
            if (chunk == null || !chunk.isGenerated()) return true;
        }
        return false;
    }

    private boolean columnRequiresMeshing(int chunkX, int playerChunkY, int chunkZ, int lod) {
        World world = Game.getWorld();
        Player player = Game.getPlayer();
        for (int chunkY = playerChunkY - RENDER_DISTANCE_Y; chunkY < playerChunkY + RENDER_DISTANCE_Y + 1; chunkY++) {
            int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
            Chunk chunk = world.getChunk(chunkIndex, lod);
            if (chunk == null || !player.getMeshCollector().isMeshed(chunkIndex, lod)) return true;
        }
        return false;
    }


    private final ThreadPoolExecutor executor;

    private record Generator(int chunkX, int playerChunkY, int chunkZ, int lod) implements Runnable {

        @Override
        public void run() {

            GenerationData generationData = new GenerationData(chunkX, chunkZ, lod);
            World world = Game.getWorld();

            for (int chunkY = playerChunkY - RENDER_DISTANCE_Y - 1; chunkY < playerChunkY + RENDER_DISTANCE_Y + 2; chunkY++) {
                try {
                    final long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ);
                    Chunk chunk = world.getChunk(chunkX, chunkY, chunkZ, lod);

                    if (chunk == null) {
//                        chunk = FileManager.getChunk(expectedId, lod);
//                        if (chunk == null)
                        chunk = new Chunk(chunkX, chunkY, chunkZ, lod);

                        world.storeChunk(chunk);
                    } else if (chunk.ID != expectedId) {
                        System.err.println("found chunk has wrong id found LOD:" + chunk.LOD + " expected:" + lod);
                        System.err.printf("expected %s %s %s%n", chunkX, chunkY, chunkZ);
                        System.err.printf("found    %s %s %s%n", chunk.X, chunk.Y, chunk.Z);

//                        chunk = FileManager.getChunk(expectedId, lod);
//                        if (chunk == null)
                        chunk = new Chunk(chunkX, chunkY, chunkZ, lod);

                        world.storeChunk(chunk);
                    }
                    if (!chunk.isGenerated()) WorldGeneration.generate(chunk, generationData);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    System.err.println("Generator:");
                    System.err.println(exception.getClass());
                    System.err.println(chunkX + " " + chunkY + " " + chunkZ);
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
                    int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
                    Chunk chunk = world.getChunk(chunkIndex, lod);
                    if (chunk == null) {
                        System.err.println("to mesh chunk is null");
                        System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                        continue;
                    }
                    if (!chunk.isGenerated()) {
                        System.err.println("to mesh chunk hasn't been generated");
                        System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                        WorldGeneration.generate(chunk);
                    }
                    if (meshCollector.isMeshed(chunkIndex, lod)) continue;
                    meshGenerator.generateMesh(chunk);

                } catch (Exception exception) {
                    System.err.println("Meshing:");
                    System.err.println(exception.getClass());
                    exception.printStackTrace();
                    System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                }
            }
        }
    }
}
