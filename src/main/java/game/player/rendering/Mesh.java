package game.player.rendering;

import core.utils.Vector3l;

import static game.utils.Constants.*;

public record Mesh(int[] opaqueVertices, int[] vertexCounts,
                   int[] transparentVertices, int waterVertexCount, int glassVertexCount,
                   long chunkX, long chunkY, long chunkZ, int lod,
                   AABB occluder, AABB occludee) {

    public Vector3l getWorldCoordinate() {
        return new Vector3l(chunkX << CHUNK_SIZE_BITS, chunkY << CHUNK_SIZE_BITS, chunkZ << CHUNK_SIZE_BITS);
    }

    public int getOpaqueByteSize() {
        return opaqueVertices.length << 2;
    }

    public int getTransparentByteSize() {
        return transparentVertices.length << 2;
    }
}
