package game.server;

import core.utils.FileManager;
import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;
import game.utils.Status;
import game.utils.Utils;

import java.io.File;

import static game.utils.Constants.*;

public final class World {

    public World(long seed) {
        WorldGeneration.SEED = seed;
        chunks = new Chunk[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    }

    public static void init() {
        ChunkSaver.generateHigherLODs();
        Game.getServer().loadImmediateSurroundings();
    }

    public Chunk getChunk(int chunkX, int chunkY, int chunkZ, int lod) {
        return chunks[lod][Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod)];
    }

    public Chunk getChunk(int chunkIndex, int lod) {
        return chunks[lod][chunkIndex];
    }

    public Chunk[] getLod(int lod) {
        return chunks[lod];
    }

    public void storeChunk(Chunk chunk) {
        chunks[chunk.LOD][chunk.getIndex()] = chunk;
    }

    public Status getGenerationStatus(int chunkX, int chunkY, int chunkZ, int lod) {
        Chunk chunk = getChunk(chunkX, chunkY, chunkZ, lod);
        if (chunk == null) return Status.NOT_STARTED;
        return chunk.getGenerationStatus();
    }

    public byte getMaterial(int x, int y, int z, int lod) {
        Chunk chunk = getChunk(x >> CHUNK_SIZE_BITS, y >> CHUNK_SIZE_BITS, z >> CHUNK_SIZE_BITS, lod);
        if (chunk == null) return OUT_OF_WORLD;
        return chunk.getSaveMaterial(x & CHUNK_SIZE_MASK, y & CHUNK_SIZE_MASK, z & CHUNK_SIZE_MASK);
    }

    public void setNull(int chunkIndex, int lod) {
        chunks[lod][chunkIndex] = null;
    }

    public void cleanUp() {
        ChunkSaver saver = new ChunkSaver();
        for (Chunk[] lod : chunks)
            for (Chunk chunk : lod) {
                if (chunk == null || !chunk.isModified()) continue;
                saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
            }

        deleteHigherLODs(LOD_COUNT - 1);
    }

    public static void deleteHigherLODs(int maxKeptLod) {
        File[] lodFiles = FileManager.getChildren(new File(ChunkSaver.getSaveFileLocation()));
        for (File file : lodFiles) {
            String fileLod = file.getName();
            if (!Utils.isInteger(fileLod, 10) || Integer.parseInt(fileLod) > maxKeptLod) FileManager.delete(file);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private String name;
    private final Chunk[][] chunks;
}
