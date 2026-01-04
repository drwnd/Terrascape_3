package game.server;

import static game.utils.Constants.*;

public record ChunkID(int chunkX, int chunkY, int chunkZ) {

    public ChunkID(int chunkX, int chunkY, int chunkZ, int lod) {
        this(chunkX & MAX_CHUNKS_MASK >> lod, chunkY & MAX_CHUNKS_MASK >> lod, chunkZ & MAX_CHUNKS_MASK >> lod);
    }

    @Override
    public String toString() {
        return "%s_%s_%s".formatted(Integer.toHexString(chunkX), Integer.toHexString(chunkY), Integer.toHexString(chunkZ));
    }
}
