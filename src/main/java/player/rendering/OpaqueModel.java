package player.rendering;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static utils.Constants.*;

public record OpaqueModel(int X, int Y, int Z, int LOD, int verticesBuffer, int[] vertexCounts, int[] toRenderVertexCounts, int[] indices) {

    public static final int FACE_COUNT = 6;

    public OpaqueModel(Vector3i position, int[] vertexCounts, int verticesBuffer, int lod) {
        this(position.x, position.y, position.z, lod, verticesBuffer, vertexCounts, new int[FACE_COUNT], getIndices(vertexCounts));
    }

    public int[] getVertexCounts(int playerChunkX, int playerChunkY, int playerChunkZ) {
        assert toRenderVertexCounts != null && vertexCounts != null;

        int modelChunkX = X >> CHUNK_SIZE_BITS + LOD;
        int modelChunkY = Y >> CHUNK_SIZE_BITS + LOD;
        int modelChunkZ = Z >> CHUNK_SIZE_BITS + LOD;
        playerChunkX >>= LOD;
        playerChunkY >>= LOD;
        playerChunkZ >>= LOD;

        toRenderVertexCounts[WEST] = playerChunkX >= modelChunkX ? vertexCounts[WEST] : 0;
        toRenderVertexCounts[EAST] = playerChunkX <= modelChunkX ? vertexCounts[EAST] : 0;
        toRenderVertexCounts[TOP] = playerChunkY >= modelChunkY ? vertexCounts[TOP] : 0;
        toRenderVertexCounts[BOTTOM] = playerChunkY <= modelChunkY ? vertexCounts[BOTTOM] : 0;
        toRenderVertexCounts[NORTH] = playerChunkZ >= modelChunkZ ? vertexCounts[NORTH] : 0;
        toRenderVertexCounts[SOUTH] = playerChunkZ <= modelChunkZ ? vertexCounts[SOUTH] : 0;
        return toRenderVertexCounts;
    }

    public int[] getIndices() {
        return indices;
    }

    private static int[] getIndices(int[] vertexCounts) {
        int[] indices = new int[FACE_COUNT];
        if (vertexCounts == null) return indices;
        indices[0] = 0;
        for (int index = 1; index < FACE_COUNT; index++)
            indices[index] = indices[index - 1] + vertexCounts[index - 1];
        return indices;
    }

    public boolean containsGeometry() {
        return vertexCounts != null;
    }

    public void delete() {
        GL46.glDeleteBuffers(verticesBuffer);
    }
}
