package game.player.rendering;

import org.joml.Vector3i;

import static game.utils.Constants.*;

public record Mesh(int[] opaqueVertices, int[] vertexCounts,
                   int[] transparentVertices, int waterVertexCount, int glassVertexCount,
                   int chunkX, int chunkY, int chunkZ, int lod) {

    public Vector3i getWorldCoordinate() {
        return new Vector3i(chunkX << CHUNK_SIZE_BITS, chunkY << CHUNK_SIZE_BITS, chunkZ << CHUNK_SIZE_BITS);
    }

    public int getOpaqueByteSize() {
        return opaqueVertices.length << 2;
    }

    public int getTransparentByteSize() {
        return transparentVertices.length << 2;
    }
}
