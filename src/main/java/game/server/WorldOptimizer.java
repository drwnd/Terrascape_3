package game.server;

import core.utils.FileManager;
import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;
import game.server.saving.WorldSaver;

import java.io.File;
import java.util.Arrays;

public final class WorldOptimizer {

    public static void optimize(File saveFile) {
        String worldName = saveFile.getName();
        World world = new WorldSaver().load(WorldSaver.getSaveFileLocation(worldName));
        world.setName(worldName);
        if (!Game.setTemporaryWorld(world)) return;

        long start = System.nanoTime();
        deleteHigherLODs();
        System.out.printf("Deleted higher LODs. Took %sms%n", (System.nanoTime() - start) / 1_000_000);

        start = System.nanoTime();
        int deletedChunkCount = deleteRedundantChunks();
        System.out.printf("Deleted %s redundant chunks. Took %sms%n", deletedChunkCount, (System.nanoTime() - start) / 1_000_000);

        ChunkSaver.generateHigherLODs();

        Game.removeTemporaryWorld();
    }

    private static void deleteHigherLODs() {
        Game.getWorld().deleteHigherLODs(0);
    }

    private static int deleteRedundantChunks() {
        File[] chunkFiles = FileManager.getChildren(new File(ChunkSaver.getSaveFileLocation(0)));
        if (chunkFiles == null) return 0;
        ChunkSaver saver = new ChunkSaver();

        int counter = 0;
        for (File chunkFile : chunkFiles) {
            if (chunkFile == null) continue;
            boolean deleted = deleteIfRedundant(saver, chunkFile);
            if (deleted) counter++;
        }
        return counter;
    }

    private static boolean deleteIfRedundant(ChunkSaver saver, File chunkFile) {
        Chunk savedChunk = saver.load(chunkFile.getPath());
        Chunk worldChunk = new Chunk(savedChunk.X, savedChunk.Y, savedChunk.Z, savedChunk.LOD);

        WorldGeneration.generate(worldChunk);

        byte[] savedData = savedChunk.getMaterials().getBytes();
        byte[] worldData = worldChunk.getMaterials().getBytes();

        if (!Arrays.equals(savedData, worldData)) return false;

        FileManager.delete(chunkFile);
        Game.getWorld().setNull(worldChunk.INDEX, worldChunk.LOD);
        return true;
    }
}
