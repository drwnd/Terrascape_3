package game.server.saving;

import core.utils.FileManager;
import core.utils.Saver;
import game.server.Chunk;
import game.server.Game;
import game.server.MaterialsData;
import game.server.generation.WorldGeneration;
import game.utils.Status;
import game.utils.Utils;

import java.io.File;

import static game.utils.Constants.*;

public final class ChunkSaver extends Saver<Chunk> {

    public static String getSaveFileLocation(long id, int lod) {
        // return "saves/" + Game.getWorld().getName() + "/chunks/" + lod + "/" + Utils.getChunkId(chunkX, chunkY, chunkZ);
        return "saves/%s/chunks/%s/%s".formatted(Game.getWorld().getName(), lod, id);
    }

    public static String getSaveFileLocation(int lod) {
        return "saves/%s/chunks/%s".formatted(Game.getWorld().getName(), lod);
    }

    public static String getSaveFileLocation() {
        return "saves/%s/chunks".formatted(Game.getWorld().getName());
    }

    public static void generateHigherLODs() {
        long start = System.nanoTime();
        for (int lod = 1; lod < LOD_COUNT; lod++) generateLod(lod);
        System.out.printf("Finished generating all LODs. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
    }

    private static void generateLod(int lod) {
        long start = System.nanoTime();
        ChunkSaver saver = new ChunkSaver();
        int lowerLOD = lod - 1;

        File lowerLodFile = new File(getSaveFileLocation(lowerLOD));
        File thisLodFile = new File(getSaveFileLocation(lod));

        if (!lowerLodFile.exists()) return; // No stored chunks to propagate into higher LODs
        if (thisLodFile.exists()) return;   // LOD is saved from previous play session

        else thisLodFile = FileManager.loadAndCreateDirectory(thisLodFile.getPath());
        File[] lowerLodChunkFiles = FileManager.getChildren(lowerLodFile);

        if (lowerLodChunkFiles == null) {
            System.err.println("Error occurred when listing lod " + lowerLOD + " chunk files.");
            return;
        }

        for (File chunkFile : lowerLodChunkFiles) {
            Chunk chunk = saver.load(chunkFile.getPath());
            if (chunk == null) continue;
            int thisLodChunkX = chunk.X >> 1;
            int thisLodChunkY = chunk.Y >> 1;
            int thisLodChunkZ = chunk.Z >> 1;
            long thisLodChunkId = Utils.getChunkId(thisLodChunkX, thisLodChunkY, thisLodChunkZ);
            File thisLodChunkFile = new File(thisLodFile.getPath() + "/" + thisLodChunkId);
            if (thisLodChunkFile.exists()) continue;

            Chunk thisLodChunk = new Chunk(thisLodChunkX, thisLodChunkY, thisLodChunkZ, lod);
            generateChunk(thisLodChunk, saver);
            saver.save(thisLodChunk, getSaveFileLocation(thisLodChunkId, lod));
        }
        System.out.printf("Finished generating lod %s, generated from %s lowerLod chunks. Took %sms%n", lod, lowerLodChunkFiles.length, (System.nanoTime() - start) / 1_000_000);
    }

    private static void generateChunk(Chunk chunk, ChunkSaver saver) {
        WorldGeneration.generate(chunk);

        int lowLODStartX = chunk.X << 1;
        int lowLODStartY = chunk.Y << 1;
        int lowLODStartZ = chunk.Z << 1;
        int lowLOD = chunk.LOD - 1;

        Chunk chunk0 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX, lowLODStartY, lowLODStartZ), lowLOD));
        Chunk chunk1 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX, lowLODStartY, lowLODStartZ + 1), lowLOD));
        Chunk chunk2 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX, lowLODStartY + 1, lowLODStartZ), lowLOD));
        Chunk chunk3 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX, lowLODStartY + 1, lowLODStartZ + 1), lowLOD));
        Chunk chunk4 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX + 1, lowLODStartY, lowLODStartZ), lowLOD));
        Chunk chunk5 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX + 1, lowLODStartY, lowLODStartZ + 1), lowLOD));
        Chunk chunk6 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX + 1, lowLODStartY + 1, lowLODStartZ), lowLOD));
        Chunk chunk7 = saver.load(getSaveFileLocation(Utils.getChunkId(lowLODStartX + 1, lowLODStartY + 1, lowLODStartZ + 1), lowLOD));

        chunk.getMaterials().storeLowerLODChunks(chunk0, chunk1, chunk2, chunk3, chunk4, chunk5, chunk6, chunk7);
    }


    public Chunk load(int chunkX, int chunkY, int chunkZ, int lod) {
        long expectedID = Utils.getChunkId(chunkX, chunkY, chunkZ);
        Chunk chunk = Game.getWorld().getChunk(chunkX, chunkY, chunkZ, lod);

        if (chunk == null) return load(chunkX, chunkY, chunkZ, lod, expectedID);
        if (chunk.ID != expectedID) {
            if (chunk.isModified()) save(chunk, getSaveFileLocation(chunk.ID, chunk.LOD));
            return load(chunkX, chunkY, chunkZ, lod, expectedID);
        }
        return chunk;
    }

    private Chunk load(int chunkX, int chunkY, int chunkZ, int lod, long id) {
        Chunk chunk = load(getSaveFileLocation(id, lod));
        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, lod);
        else chunk.setGenerationStatus(Status.DONE);

        Game.getWorld().storeChunk(chunk);
        return chunk;
    }

    @Override
    protected void save(Chunk chunk) {
        saveInt(chunk.X);
        saveInt(chunk.Y);
        saveInt(chunk.Z);
        saveInt(chunk.LOD);
        saveByteArray(chunk.getMaterials().getBytes());
    }

    @Override
    protected Chunk load() {
        int x = loadInt();
        int y = loadInt();
        int z = loadInt();
        int lod = loadInt();
        byte[] materials = loadByteArray();

        Chunk chunk = new Chunk(x, y, z, lod);
        chunk.setMaterials(new MaterialsData(CHUNK_SIZE_BITS, materials));
        return chunk;
    }

    @Override
    protected Chunk getDefault() {
        return null;
    }

    @Override
    protected int getVersionNumber() {
        return 0;
    }
}
