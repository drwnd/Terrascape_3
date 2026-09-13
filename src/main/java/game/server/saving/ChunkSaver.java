package game.server.saving;

import core.rendering_api.Debug;
import core.utils.FileManager;
import core.utils.Saver;
import game.server.Chunk;
import game.server.ChunkID;
import game.server.Game;
import game.server.materials_data.MaterialsData;
import game.server.generation.WorldGeneration;
import game.utils.Status;

import java.io.File;
import java.nio.file.Path;

import static game.utils.Constants.*;

public final class ChunkSaver extends Saver<Chunk> {

    public static Path getSaveFileLocation(ChunkID id, int lod) {
        return Path.of("saves", Game.getWorld().getName(), "chunks", String.valueOf(lod), String.valueOf(id));
    }

    public static Path getSaveFileLocation(int lod) {
        return Path.of("saves", Game.getWorld().getName(), "chunks", String.valueOf(lod));
    }

    public static Path getSaveFileLocation() {
        return Path.of("saves", Game.getWorld().getName(), "chunks");
    }

    public static void generateHigherLODs() {
        long start = System.nanoTime();
        for (int lod = 1, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) generateLod(lod);
        Debug.log("Finished generating all LODs. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
    }

    private static void generateLod(int lod) {
        long start = System.nanoTime();
        ChunkSaver saver = new ChunkSaver();
        int lowerLOD = lod - 1;

        File lowerLodFile = getSaveFileLocation(lowerLOD).toFile();
        File thisLodFile = getSaveFileLocation(lod).toFile();

        if (!lowerLodFile.exists()) return; // No stored chunks to propagate into higher LODs
        if (thisLodFile.exists()) return;   // LOD is saved from previous play session

        else thisLodFile = FileManager.loadAndCreateDirectory(thisLodFile.toPath());
        File[] lowerLodChunkFiles = FileManager.getChildren(lowerLodFile.toPath());

        for (File chunkFile : lowerLodChunkFiles) {
            Chunk chunk = saver.load(chunkFile.toPath());
            if (chunk == null) continue;
            long thisLodChunkX = chunk.X >> 1;
            long thisLodChunkY = chunk.Y >> 1;
            long thisLodChunkZ = chunk.Z >> 1;
            ChunkID thisLodChunkId = new ChunkID(thisLodChunkX, thisLodChunkY, thisLodChunkZ, lod);
            File thisLodChunkFile = thisLodFile.toPath().resolve(String.valueOf(thisLodChunkId)).toFile();
            if (thisLodChunkFile.exists()) continue;

            Chunk thisLodChunk = new Chunk(thisLodChunkX, thisLodChunkY, thisLodChunkZ, lod);
            generateChunk(thisLodChunk, saver);
            saver.save(thisLodChunk, getSaveFileLocation(thisLodChunkId, lod));
        }
        Debug.log("Finished generating lod %s, generated from %s lowerLod chunks. Took %sms%n", lod, lowerLodChunkFiles.length, (System.nanoTime() - start) / 1_000_000);
    }

    private static void generateChunk(Chunk chunk, ChunkSaver saver) {
        WorldGeneration.generate(chunk);
        Game.getWorld().storeChunk(chunk);

        long lowLODStartX = chunk.X << 1;
        long lowLODStartY = chunk.Y << 1;
        long lowLODStartZ = chunk.Z << 1;
        int lowLOD = chunk.LOD - 1;

        Chunk chunk0 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX, lowLODStartY, lowLODStartZ, lowLOD), lowLOD));
        Chunk chunk1 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX, lowLODStartY, lowLODStartZ + 1, lowLOD), lowLOD));
        Chunk chunk2 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX, lowLODStartY + 1, lowLODStartZ, lowLOD), lowLOD));
        Chunk chunk3 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX, lowLODStartY + 1, lowLODStartZ + 1, lowLOD), lowLOD));
        Chunk chunk4 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX + 1, lowLODStartY, lowLODStartZ, lowLOD), lowLOD));
        Chunk chunk5 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX + 1, lowLODStartY, lowLODStartZ + 1, lowLOD), lowLOD));
        Chunk chunk6 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX + 1, lowLODStartY + 1, lowLODStartZ, lowLOD), lowLOD));
        Chunk chunk7 = saver.load(getSaveFileLocation(new ChunkID(lowLODStartX + 1, lowLODStartY + 1, lowLODStartZ + 1, lowLOD), lowLOD));

        chunk.getMaterials().storeLowerLODChunks(chunk0, chunk1, chunk2, chunk3, chunk4, chunk5, chunk6, chunk7);
    }


    public Chunk loadAndGenerate(long chunkX, long chunkY, long chunkZ, int lod) {
        Chunk chunk = load(chunkX, chunkY, chunkZ, lod);
        WorldGeneration.generate(chunk);
        return chunk;
    }

    public Chunk load(long chunkX, long chunkY, long chunkZ, int lod) {
        ChunkID expectedID = new ChunkID(chunkX, chunkY, chunkZ, lod);
        Chunk chunk = Game.getWorld().getChunk(chunkX, chunkY, chunkZ, lod);

        if (chunk == null) return load(chunkX, chunkY, chunkZ, lod, expectedID);
        if (!chunk.ID.equals(expectedID)) {
            if (chunk.isModified()) save(chunk, getSaveFileLocation(chunk.ID, chunk.LOD));
            return load(chunkX, chunkY, chunkZ, lod, expectedID);
        }
        return chunk;
    }

    private Chunk load(long chunkX, long chunkY, long chunkZ, int lod, ChunkID id) {
        Chunk chunk = load(getSaveFileLocation(id, lod));
        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, lod);
        else chunk.setGenerationStatus(Status.DONE);

        Game.getWorld().storeChunk(chunk);
        return chunk;
    }

    @Override
    protected void save(Chunk chunk) {
        saveLong(chunk.X);
        saveLong(chunk.Y);
        saveLong(chunk.Z);
        saveInt(chunk.LOD);
        saveByteArray(chunk.getMaterials().getBytes());
    }

    @Override
    protected Chunk load() {
        long x = loadLong();
        long y = loadLong();
        long z = loadLong();
        int lod = loadInt();
        byte[] materials = loadByteArray();

        Chunk chunk = new Chunk(x, y, z, lod);
        MaterialsData materialsData = new MaterialsData(CHUNK_SIZE_BITS, materials);
        materialsData.recomputeTypes();
        chunk.setMaterials(materialsData);
        return chunk;
    }

    @Override
    protected Chunk getDefault() {
        return null;
    }

    @Override
    protected int getVersionNumber() {
        return 2;
    }

    @Override
    protected Chunk loadOldVersion(int versionNumber) {
        if (versionNumber == 1) {
            int x = loadInt();
            int y = loadInt();
            int z = loadInt();
            int lod = loadInt();
            byte[] materials = loadByteArray();

            Chunk chunk = new Chunk(x, y, z, lod);
            chunk.setMaterials(new MaterialsData(CHUNK_SIZE_BITS, materials));
            return chunk;
        }
        return getDefault();
    }
}
