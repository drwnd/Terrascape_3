package game.server;

import static game.utils.Constants.*;

public record ChunkID(long chunkX, long chunkY, long chunkZ, int lod) {

    public ChunkID(long chunkX, long chunkY, long chunkZ, int lod) {
        long chunksMask = MAX_CHUNKS_MASK >> lod;
        this.chunkX = chunkX & chunksMask;
        this.chunkY = chunkY & chunksMask;
        this.chunkZ = chunkZ & chunksMask;
        this.lod = lod;
    }

    public boolean equals(long chunkX, long chunkY, long chunkZ) {
        long chunksMask = MAX_CHUNKS_MASK >> lod;
        return this.chunkX == (chunkX & chunksMask)
                && this.chunkY == (chunkY & chunksMask)
                && this.chunkZ == (chunkZ & chunksMask);
    }

    @Override
    public String toString() {
        return "%s_%s_%s".formatted(Long.toHexString(chunkX), Long.toHexString(chunkY), Long.toHexString(chunkZ));
    }
}
