package game.server;

import core.rendering_api.Debug;
import core.utils.FileManager;
import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;
import core.utils.MainThread;

import java.io.File;
import java.util.Arrays;

public final class WorldOptimizer {

    @MainThread
    public static void optimize(World world) {
        if (!Game.setTemporaryWorld(world)) return;

        long start = System.nanoTime();
        deleteHigherLODs();
        Debug.log("Deleted higher LODs. Took %sms%n", (System.nanoTime() - start) / 1_000_000);

        start = System.nanoTime();
        int deletedChunkCount = deleteRedundantChunks();
        Debug.log("Deleted %s redundant chunks. Took %sms%n", deletedChunkCount, (System.nanoTime() - start) / 1_000_000);

        ChunkSaver.generateHigherLODs();

        Game.removeTemporaryWorld();
    }

    @MainThread
    private static void deleteHigherLODs() {
        World.deleteHigherLODs(0);
    }

    @MainThread
    private static int deleteRedundantChunks() {
        File[] chunkFiles = FileManager.getChildren(ChunkSaver.getSaveFileLocation(0));
        ChunkSaver saver = new ChunkSaver();

        int counter = 0;
        for (File chunkFile : chunkFiles) {
            if (chunkFile == null) continue;
            boolean deleted = deleteIfRedundant(saver, chunkFile);
            if (deleted) counter++;
        }
        return counter;
    }

    @MainThread
    private static boolean deleteIfRedundant(ChunkSaver saver, File chunkFile) {
        Chunk savedChunk = saver.load(chunkFile.toPath());
        if (savedChunk == null) return delete(chunkFile);
        Chunk worldChunk = new Chunk(savedChunk.X, savedChunk.Y, savedChunk.Z, savedChunk.LOD);

        WorldGeneration.generate(worldChunk);
        Game.getWorld().storeChunk(worldChunk);

        byte[] savedData = savedChunk.getMaterials().getBytes();
        byte[] worldData = worldChunk.getMaterials().getBytes();

        if (!Arrays.equals(savedData, worldData)) return false;

        Game.getWorld().setNull(worldChunk.INDEX, worldChunk.LOD);
        return delete(chunkFile);
    }

    @MainThread
    private static boolean delete(File chunkFile) {
        FileManager.delete(chunkFile);
        return true;
    }
}
