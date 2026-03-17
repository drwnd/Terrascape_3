package game.server;

import static game.utils.Constants.*;

public record ChunkID(long chunkX, long chunkY, long chunkZ) {

    public ChunkID(long chunkX, long chunkY, long chunkZ, int lod) {
        this(chunkX & MAX_CHUNKS_MASK >> lod, chunkY & MAX_CHUNKS_MASK >> lod, chunkZ & MAX_CHUNKS_MASK >> lod);
    }

    @Override
    public String toString() {
        return "%s_%s_%s".formatted(Long.toHexString(chunkX), Long.toHexString(chunkY), Long.toHexString(chunkZ));
    }
}
