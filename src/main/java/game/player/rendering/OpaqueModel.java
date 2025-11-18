package game.player.rendering;

import game.utils.Utils;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static game.utils.Constants.*;

public record OpaqueModel(int totalX, int totalY, int totalZ, int LOD, int verticesBuffer, int[] vertexCounts, int[] toRenderVertexCounts, int[] indices) {

    public static final int FACE_COUNT = 6;

    public OpaqueModel(Vector3i position, int[] vertexCounts, int verticesBuffer, int lod) {
        this(position.x << lod, position.y << lod, position.z << lod, lod, verticesBuffer, vertexCounts, new int[FACE_COUNT], getIndices(vertexCounts));
    }

    public int[] getVertexCounts(int cameraChunkX, int cameraChunkY, int cameraChunkZ) {
        cameraChunkX >>= LOD;
        cameraChunkY >>= LOD;
        cameraChunkZ >>= LOD;
        int modelChunkX = Utils.getWrappedPosition(chunkX(), cameraChunkX, MAX_CHUNKS_XZ + 1 >> LOD);
        int modelChunkY = Utils.getWrappedPosition(chunkY(), cameraChunkY, MAX_CHUNKS_Y + 1 >> LOD);
        int modelChunkZ = Utils.getWrappedPosition(chunkZ(), cameraChunkZ, MAX_CHUNKS_XZ + 1 >> LOD);

        toRenderVertexCounts[WEST] = cameraChunkX >= modelChunkX ? vertexCounts[WEST] : 0;
        toRenderVertexCounts[EAST] = cameraChunkX <= modelChunkX ? vertexCounts[EAST] : 0;
        toRenderVertexCounts[TOP] = cameraChunkY >= modelChunkY ? vertexCounts[TOP] : 0;
        toRenderVertexCounts[BOTTOM] = cameraChunkY <= modelChunkY ? vertexCounts[BOTTOM] : 0;
        toRenderVertexCounts[NORTH] = cameraChunkZ >= modelChunkZ ? vertexCounts[NORTH] : 0;
        toRenderVertexCounts[SOUTH] = cameraChunkZ <= modelChunkZ ? vertexCounts[SOUTH] : 0;
        return toRenderVertexCounts;
    }

    public int[] getIndices() {
        return indices;
    }

    public boolean isEmpty() {
        return vertexCounts == null;
    }

    public void delete() {
        GL46.glDeleteBuffers(verticesBuffer);
    }

    public int chunkX() {
        return totalX >> CHUNK_SIZE_BITS + LOD;
    }

    public int chunkY() {
        return totalY >> CHUNK_SIZE_BITS + LOD;
    }

    public int chunkZ() {
        return totalZ >> CHUNK_SIZE_BITS + LOD;
    }


    private static int[] getIndices(int[] vertexCounts) {
        int[] indices = new int[FACE_COUNT];
        if (vertexCounts == null) return indices;
        indices[0] = 0;
        for (int index = 1; index < FACE_COUNT; index++)
            indices[index] = indices[index - 1] + vertexCounts[index - 1];
        return indices;
    }
}
