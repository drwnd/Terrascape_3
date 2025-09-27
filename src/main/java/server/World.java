package server;

import server.saving.ChunkSaver;
import utils.Utils;

import static utils.Constants.*;

public final class World {

    public World() {
        chunks = new Chunk[RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    }

    public Chunk getChunk(int chunkX, int chunkY, int chunkZ, int lod) {
        return chunks[Utils.getChunkIndex(chunkX, chunkY, chunkZ)];
    }

    public Chunk getChunk(int chunkIndex, int lod) {
        return chunks[chunkIndex];
    }

    public Chunk[] getLod(int lod) {
        return chunks;
    }

    public void storeChunk(Chunk chunk) {
        chunks[chunk.getIndex()] = chunk;
    }

    public byte getMaterial(int x, int y, int z, int lod) {
        Chunk chunk = getChunk(x >> CHUNK_SIZE_BITS, y >> CHUNK_SIZE_BITS, z >> CHUNK_SIZE_BITS, lod);
        if (chunk == null) return OUT_OF_WORLD;
        return chunk.getSaveMaterial(x & CHUNK_SIZE_MASK, y & CHUNK_SIZE_MASK, z & CHUNK_SIZE_MASK);
    }

    public void setNull(int chunkIndex, int lod) {
        chunks[chunkIndex] = null;
    }

    public void cleanUp() {
        ChunkSaver saver = new ChunkSaver();
        for (Chunk chunk : chunks) {
            if (chunk == null || !chunk.isModified()) continue;
            saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
        }
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private String name;
    private final Chunk[] chunks;
}
