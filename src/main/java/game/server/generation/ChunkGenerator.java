package game.server.generation;

import core.rendering_api.Debug;
import core.utils.Vector3l;

import game.player.rendering.Mesh;
import game.player.rendering.MeshCollector;
import game.player.rendering.MeshGenerator;
import game.server.*;
import game.server.saving.ChunkSaver;
import game.settings.IntSettings;
import game.utils.Status;
import game.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

import static game.utils.Constants.*;

public final class ChunkGenerator {

    public ChunkGenerator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    public static void loadImmediateSurroundings() {
        Vector3l playerPosition = Game.getPlayer().getPosition().longPosition();

        long playerChunkX = playerPosition.x >>> CHUNK_SIZE_BITS;
        long playerChunkY = playerPosition.y >>> CHUNK_SIZE_BITS;
        long playerChunkZ = playerPosition.z >>> CHUNK_SIZE_BITS;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
        ChunkSaver saver = new ChunkSaver();
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX + 0, playerChunkY, playerChunkZ + 1, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX + 0, playerChunkY, playerChunkZ + 0, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX + 0, playerChunkY, playerChunkZ - 1, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX + 1, playerChunkY, playerChunkZ + 1, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX + 1, playerChunkY, playerChunkZ + 0, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX + 1, playerChunkY, playerChunkZ - 1, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX - 1, playerChunkY, playerChunkZ + 1, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX - 1, playerChunkY, playerChunkZ + 0, 0)));
        executor.submit(new Generator(getToGenerateChunks(saver, playerChunkX - 1, playerChunkY, playerChunkZ - 1, 0)));

        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            Debug.err("Executor failed to generate immediate surroundings.");
        }
    }

    public void restart() {
        Vector3l playerChunkPosition = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (this) {
            for (Future<Mesh[]> future : meshHandlerFutures) future.cancel(false);
            for (Future<?> future : generatorFutures) future.cancel(false);
        }
        Server.unloadDistantChunks(playerChunkPosition);
        submitGeneratorTasks(playerChunkPosition.x, playerChunkPosition.y, playerChunkPosition.z);
    }

    public void cleanUp() {
        waitUntilHalt();
    }

    public void updateGameTick() {
        meshHandlerFutures.removeIf(Future::isCancelled);
        generatorFutures.removeIf(Future::isCancelled);
        storeFinishedMeshes(meshHandlerFutures);
        Vector3l playerChunkPosition = Game.getPlayer().getPosition().getChunkCoordinate();
        for (Future<Mesh[]> future : meshHandlerFutures) future.cancel(false);
        submitMeshingTasks(playerChunkPosition.x, playerChunkPosition.y, playerChunkPosition.z);
    }

    private static void storeFinishedMeshes(ArrayList<Future<Mesh[]>> futures) {
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        for (Iterator<Future<Mesh[]>> iterator = futures.iterator(); iterator.hasNext(); ) {
            Future<Mesh[]> future = iterator.next();
            if (!future.isDone()) continue;
            iterator.remove();

            try {
                Mesh[] meshes = future.get();
                for (Mesh mesh : meshes) {
                    if (mesh == null) continue;
                    meshCollector.setMeshed(true, Utils.getChunkIndex(mesh.chunkX(), mesh.chunkY(), mesh.chunkZ(), mesh.lod()), mesh.lod());
                    meshCollector.queueMesh(mesh);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void waitUntilHalt() {
        synchronized (this) {
            for (Future<Mesh[]> future : meshHandlerFutures) future.cancel(false);
            for (Future<?> future : generatorFutures) future.cancel(false);
        }
        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Debug.err("Crashed when awaiting termination");
            e.printStackTrace();
        }
    }

    private void submitGeneratorTasks(long playerChunkX, long playerChunkY, long playerChunkZ) {
        ChunkSaver saver = new ChunkSaver();
        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            long lodPlayerX = playerChunkX >> lod;
            long lodPlayerY = playerChunkY >> lod;
            long lodPlayerZ = playerChunkZ >> lod;

            for (int ring = 0; ring <= IntSettings.RENDER_DISTANCE.value() + 1; ring++)
                submitRingGeneration(saver, lodPlayerX, lodPlayerY, lodPlayerZ, ring, lod);
        }
    }

    private void submitMeshingTasks(long playerChunkX, long playerChunkY, long playerChunkZ) {
        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            long lodPlayerX = playerChunkX >> lod;
            long lodPlayerY = playerChunkY >> lod;
            long lodPlayerZ = playerChunkZ >> lod;

            for (int ring = 0; ring <= IntSettings.RENDER_DISTANCE.value(); ring++)
                submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, ring, lod);
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

    private void submitRingGeneration(ChunkSaver saver, long playerChunkX, long playerChunkY, long playerChunkZ, int ring, int lod) {
        if (ring == 0) {
            submitColumnGeneration(saver, playerChunkX, playerChunkY, playerChunkZ, lod);
            return;
        }

        for (int chunkX = -ring; chunkX < ring; chunkX++) submitColumnGeneration(saver, chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod);
        for (int chunkZ = ring; chunkZ > -ring; chunkZ--) submitColumnGeneration(saver, ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
        for (int chunkX = ring; chunkX > -ring; chunkX--) submitColumnGeneration(saver, chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod);
        for (int chunkZ = -ring; chunkZ < ring; chunkZ++) submitColumnGeneration(saver, -ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod);
    }

    private void submitColumnGeneration(ChunkSaver saver, long chunkX, long playerChunkY, long chunkZ, int lod) {
        if (executor.isShutdown()) return;
        Chunk[] requiredChunks = getToGenerateChunks(saver, chunkX, playerChunkY, chunkZ, lod);
        if (requiredChunks.length == 0) return;

        Future<?> future = executor.submit(new Generator(requiredChunks));
        generatorFutures.add(future);
    }

    private void submitColumnMeshing(long chunkX, long playerChunkY, long chunkZ, int lod) {
        if (executor.isShutdown()) return;
        ChunkNeighbors[] toMeshChunks = getToMeshChunks(chunkX, playerChunkY, chunkZ, lod);
        if (toMeshChunks.length == 0) return;

        Future<Mesh[]> future = executor.submit(new MeshHandler(toMeshChunks));
        meshHandlerFutures.add(future);
    }

    private static Chunk[] getToGenerateChunks(ChunkSaver saver, long chunkX, long playerChunkY, long chunkZ, int lod) {
        Chunk[] chunks = new Chunk[IntSettings.RENDER_DISTANCE.value() * 2 + 3];

        int count = 0;
        for (long chunkY = playerChunkY - IntSettings.RENDER_DISTANCE.value() - 1; chunkY != playerChunkY + IntSettings.RENDER_DISTANCE.value() + 2; chunkY++) {
            Chunk chunk = saver.load(chunkX, chunkY, chunkZ, lod);
            if (chunk.getGenerationStatus() != Status.NOT_STARTED) continue;
            chunk.setGenerationStatus(Status.IN_PROGRESS);
            chunks[count++] = chunk;
        }

        if (count == chunks.length) return chunks;
        Chunk[] requiredChunks = new Chunk[count];
        System.arraycopy(chunks, 0, requiredChunks, 0, count);
        return requiredChunks;
    }

    private static ChunkNeighbors[] getToMeshChunks(long chunkX, long playerChunkY, long chunkZ, int lod) {
        World world = Game.getWorld();
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        ChunkNeighbors[] chunks = new ChunkNeighbors[IntSettings.RENDER_DISTANCE.value() * 2 + 1];

        int count = 0;
        for (long chunkY = playerChunkY - IntSettings.RENDER_DISTANCE.value(); chunkY != playerChunkY + IntSettings.RENDER_DISTANCE.value() + 1; chunkY++) {
            int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
            Chunk chunk = world.getChunk(chunkIndex, lod);
            if (chunk == null || meshCollector.isMeshed(chunkIndex, lod)) continue;
            ChunkNeighbors neighbors = chunk.getNeighbors();
            if (neighbors.areUnGenerated()) continue;
            chunks[count++] = neighbors;
        }

        if (count == chunks.length) return chunks;
        ChunkNeighbors[] requiredChunks = new ChunkNeighbors[count];
        System.arraycopy(chunks, 0, requiredChunks, 0, count);
        return requiredChunks;
    }


    private final ThreadPoolExecutor executor;
    private final ArrayList<Future<Mesh[]>> meshHandlerFutures = new ArrayList<>();
    private final ArrayList<Future<?>> generatorFutures = new ArrayList<>();

    private record Generator(Chunk[] result) implements Runnable {

        @Override
        public void run() {

            long chunkX = result[0].X;
            long chunkZ = result[0].Z;
            int lod = result[0].LOD;

            GenerationData generationData;
            try {
                generationData = new GenerationData(chunkX, chunkZ, lod);
            } catch (Exception exception) {
                Debug.err("Failed to create GenerationData");
                Debug.err(exception.getClass());
                exception.printStackTrace();
                Debug.err("X:%d Z:%d%n", chunkX, chunkZ);
                return;
            }

            for (Chunk chunk : result) {
                try {
                    WorldGeneration.generate(chunk, generationData);
                } catch (Exception exception) {
                    Debug.err("Generation:");
                    Debug.err(exception.getClass());
                    exception.printStackTrace();
                    Debug.err("%d %d %d%n", chunk.X, chunk.Y, chunk.Z);
                }
            }
        }
    }

    private record MeshHandler(ChunkNeighbors[] chunks) implements Callable<Mesh[]> {

        @Override
        public Mesh[] call() {

            MeshGenerator meshGenerator = new MeshGenerator();
            Mesh[] result = new Mesh[chunks.length];

            for (int index = 0; index < result.length; index++) {
                try {
                    result[index] = meshGenerator.generateMesh(chunks[index]);
                } catch (Exception exception) {
                    Debug.err("Meshing:");
                    Debug.err(exception.getClass());
                    exception.printStackTrace();
                    Debug.err("%d %d %d%n", chunks[index].center().X, chunks[index].center().Y, chunks[index].center().Z);
                }
            }
            return result;
        }
    }
}
