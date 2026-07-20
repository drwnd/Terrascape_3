package game.player.rendering;

import core.utils.Vector3l;

import static game.utils.Constants.*;

/**
 * Performs mesh.
 *
 * @param opaqueVertices parameter
 * @param vertexCounts parameter
 * @param transparentVertices parameter
 * @param transparentVertexCount parameter
 * @param glassVertexCount parameter
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @param occluder parameter
 * @param occludee parameter
 * @return result
 */
public record Mesh(int[] opaqueVertices, int[] vertexCounts,
                   int[] transparentVertices, int transparentVertexCount, int glassVertexCount,
                   long chunkX, long chunkY, long chunkZ, int lod,
                   AABB occluder, AABB occludee) {

    public Mesh(long chunkX, long chunkY, long chunkZ, int lod) {
        this(null, null, null, 0, 0, chunkX, chunkY,chunkZ, lod, AABB.newMinChunkAABB(), AABB.newMinChunkAABB());
    }

    public Vector3l getWorldCoordinate() {
        return new Vector3l(chunkX << CHUNK_SIZE_BITS, chunkY << CHUNK_SIZE_BITS, chunkZ << CHUNK_SIZE_BITS);
    }

    public int getOpaqueByteSize() {
        if (opaqueVertices == null) return 0;
        return opaqueVertices.length << 2;
    }

    public int getTransparentByteSize() {
        if (transparentVertices == null) return 0;
        return transparentVertices.length << 2;
    }
}
