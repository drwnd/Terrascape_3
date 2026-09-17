package game.server.generation;

import core.rendering_api.Debug;
import game.server.Chunk;
import game.server.Game;
import game.server.saving.ChunkSaver;
import game.settings.IntSettings;
import game.utils.Status;
import core.utils.WorkerThread;

record ChunkColumnGenerator(long chunkX, long playerChunkY, long chunkZ, int lod) implements Runnable {

    @Override
    @WorkerThread
    public void run() {
        GenerationData generationData;
        ChunkSaver saver = new ChunkSaver();

        try {
            generationData = new GenerationData(chunkX, chunkZ, lod);
        } catch (Exception exception) {
            Debug.err("Failed to create GenerationData");
            Debug.err(exception.getClass());
            Debug.err(exception);
            Debug.err("X:%d Z:%d%n", chunkX, chunkZ);
            return;
        }

        for (long chunkY = playerChunkY - IntSettings.RENDER_DISTANCE.value() - 1; chunkY != playerChunkY + IntSettings.RENDER_DISTANCE.value() + 2; chunkY++) {
            try {
                generateChunk(saver, chunkY, generationData);
            } catch (Exception exception) {
                Debug.err("Generation:");
                Debug.err(exception.getClass());
                Debug.err(exception);
                Debug.err("%d %d %d%n", chunkX, chunkY, chunkZ);
            }
        }
    }

    @WorkerThread
    private void generateChunk(ChunkSaver saver, long chunkY, GenerationData generationData) {
        Chunk chunk = saver.load(chunkX, chunkY, chunkZ, lod);
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        WorldGeneration.generate(chunk, generationData);
        Game.getWorld().storeChunk(chunk);
    }
}
