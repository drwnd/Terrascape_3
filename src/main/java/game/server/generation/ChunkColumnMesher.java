package game.server.generation;

import core.rendering_api.Debug;
import core.utils.WorkerThread;

import game.player.rendering.MeshCollector;
import game.player.rendering.MeshGenerator;
import game.server.*;
import game.settings.IntSettings;
import game.utils.Status;
import game.utils.Utils;

record ChunkColumnMesher(long chunkX, long playerChunkY, long chunkZ, int lod) implements Runnable {

    @Override
    @WorkerThread
    public void run() {
        MeshGenerator meshGenerator = new MeshGenerator();

        for (long chunkY = playerChunkY - IntSettings.RENDER_DISTANCE.value(); chunkY != playerChunkY + IntSettings.RENDER_DISTANCE.value() + 1; chunkY++) {
            try {
                generateMesh(meshGenerator, chunkY);
            } catch (Exception exception) {
                Debug.err("Meshing:");
                Debug.err(exception.getClass());
                Debug.err(exception);
                Debug.err("%d %d %d%n", chunkX, chunkY, chunkZ);
            }
        }
    }

    @WorkerThread
    private void generateMesh(MeshGenerator generator, long chunkY) {
        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
        ChunkID expectedId = new ChunkID(chunkX, chunkY, chunkZ, lod);
        Chunk chunk = Game.getWorld().getChunk(chunkIndex, lod);
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();
        ChunkNeighbors neighbors = chunk == null ? null : chunk.getNeighbors();

        boolean shouldGenerateMesh = false;
        if (chunk == null)
            Debug.err("to mesh chunk is null %d %d %d %d%n", chunkX, chunkY, chunkZ, lod);
        else if (!chunk.ID.equals(expectedId))
            Debug.err("Chunk has wrong ID %d %d %d %d is %s should be %s%n", chunkX, chunkY, chunkZ, lod, chunk.ID, expectedId);
        else if (chunk.getGenerationStatus() != Status.DONE)
            Debug.err("to mesh chunk hasn't been generated %s%d %d %d %d%n", chunk.getGenerationStatus().name(), chunkX, chunkY, chunkZ, lod);
        else if (neighbors.areUnGenerated())
            Debug.err("Chunk neighbors aren't generated %d %d %d %d%n", chunkX, chunkY, chunkZ, lod);
        else shouldGenerateMesh = !meshCollector.setMeshed(true, chunkIndex, lod);

        if (shouldGenerateMesh) meshCollector.queueMesh(generator.generateMesh(chunk, neighbors));
    }
}
