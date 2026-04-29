package game.server.generation;

import game.server.Chunk;
import game.server.Game;
import game.server.saving.ChunkSaver;
import game.utils.Status;

import static game.utils.Constants.RENDER_DISTANCE;

record JavaChunkGenerator(long chunkX, long playerChunkY, long chunkZ, int lod) implements Runnable {

    @Override
    public void run() {

        GenerationData generationData = new GenerationData(chunkX, chunkZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (long chunkY = playerChunkY - RENDER_DISTANCE - 1; chunkY != playerChunkY + RENDER_DISTANCE + 2; chunkY++) {
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
