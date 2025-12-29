package game.player.rendering;

import game.utils.Utils;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static game.utils.Constants.*;

public record OpaqueModel(int totalX, int totalY, int totalZ, int LOD, int bufferOrStart, int[] vertexCounts, int[] toRenderVertexCounts, int[] indices) {

    public static final int FACE_COUNT = 6;

    public OpaqueModel(Vector3i position, int[] vertexCounts, int bufferOrStart, int lod, boolean isBuffer) {
        this(position.x << lod, position.y << lod, position.z << lod,
                lod, bufferOrStart, vertexCounts, new int[FACE_COUNT],
                getIndices(vertexCounts, bufferOrStart, isBuffer));
    }

    public boolean isEmpty() {
        return vertexCounts == null;
    }

    public void delete() {
        GL46.glDeleteBuffers(bufferOrStart);
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

    private static int[] getIndices(int[] vertexCounts, int bufferOrStart, boolean isBuffer) {
        int[] indices = new int[FACE_COUNT];
        if (vertexCounts == null) return indices;
        indices[0] = firstIndex(bufferOrStart, isBuffer);
        for (int index = 1; index < FACE_COUNT; index++)
            indices[index] = indices[index - 1] + vertexCounts[index - 1];
        return indices;
    }

    private static int firstIndex(int bufferOrStart, boolean isBuffer) {
        if (isBuffer) return 0;
        return (bufferOrStart >> 2) * MeshGenerator.VERTICES_PER_QUAD / MeshGenerator.INTS_PER_VERTEX;
    }

    public int chunkIndex() {
        return Utils.getChunkIndex(chunkX(), chunkY(), chunkZ(), LOD);
    }
}
