package game.server;

import core.utils.FileManager;
import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;
import game.utils.Utils;

import java.io.File;

import static game.utils.Constants.*;

public final class World {

    public World(long seed) {
        WorldGeneration.SEED = seed;
        chunks = new Chunk[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    }

    public void init() {
        ChunkSaver.generateHigherLODs();
    }

    public Chunk getChunk(int chunkX, int chunkY, int chunkZ, int lod) {
        return chunks[lod][Utils.getChunkIndex(chunkX, chunkY, chunkZ)];
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

    public void loadAndGenerate(int chunkX, int chunkY, int chunkZ, int lod, ChunkSaver saver) {
        Chunk chunk = saver.load(chunkX, chunkY, chunkZ, lod);
        if (!chunk.isGenerated()) WorldGeneration.generate(chunk);
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
        for (Chunk chunk : chunks[0]) {
            if (chunk == null || !chunk.isModified()) continue;
            saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
        }

        for (int lod = 1; lod < LOD_COUNT; lod++) FileManager.delete(new File(ChunkSaver.getSaveFileLocation(lod)));
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
