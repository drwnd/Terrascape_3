package game.player.rendering;

import core.utils.IntArrayList;
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

    public void addDataWithOcclusionCulling(IntArrayList commands, int cameraChunkX, int cameraChunkY, int cameraChunkZ) {
        int modelChunkX = Utils.getWrappedPosition(chunkX(), cameraChunkX, MAX_CHUNKS_XZ_MASK + 1 >> LOD);
        int modelChunkY = Utils.getWrappedPosition(chunkY(), cameraChunkY, MAX_CHUNKS_Y_MASK + 1 >> LOD);
        int modelChunkZ = Utils.getWrappedPosition(chunkZ(), cameraChunkZ, MAX_CHUNKS_XZ_MASK + 1 >> LOD);
        boolean notNull = !isEmpty();

        addData(commands, notNull && cameraChunkZ >= modelChunkZ, NORTH);
        addData(commands, notNull && cameraChunkY >= modelChunkY, TOP);
        addData(commands, notNull && cameraChunkX >= modelChunkX, WEST);
        addData(commands, notNull && cameraChunkZ <= modelChunkZ, SOUTH);
        addData(commands, notNull && cameraChunkY <= modelChunkY, BOTTOM);
        addData(commands, notNull && cameraChunkX <= modelChunkX, EAST);
    }

    public void addDataWithoutOcclusionCulling(IntArrayList commands, int cameraChunkX, int cameraChunkY, int cameraChunkZ) {
        if (isEmpty()) return;
        int modelChunkX = Utils.getWrappedPosition(chunkX(), cameraChunkX, MAX_CHUNKS_XZ_MASK + 1 >> LOD);
        int modelChunkY = Utils.getWrappedPosition(chunkY(), cameraChunkY, MAX_CHUNKS_Y_MASK + 1 >> LOD);
        int modelChunkZ = Utils.getWrappedPosition(chunkZ(), cameraChunkZ, MAX_CHUNKS_XZ_MASK + 1 >> LOD);

        if (cameraChunkZ >= modelChunkZ) addData(commands, NORTH);
        if (cameraChunkY >= modelChunkY) addData(commands, TOP);
        if (cameraChunkX >= modelChunkX) addData(commands, WEST);
        if (cameraChunkZ <= modelChunkZ) addData(commands, SOUTH);
        if (cameraChunkY <= modelChunkY) addData(commands, BOTTOM);
        if (cameraChunkX <= modelChunkX) addData(commands, EAST);
    }

    public boolean isEmpty() {
        return vertexCounts == null || indices == null;
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


    private void addData(IntArrayList commands, boolean isVisible, int side) {
        commands.add(isVisible ? vertexCounts[side] : 0);
        commands.add(0);
        commands.add(isVisible ? indices[side] : 0);
        commands.add(0);
    }

    private void addData(IntArrayList commands, int side) {
        commands.add(vertexCounts[side]);
        commands.add(1);
        commands.add(indices[side]);
        commands.add(0);
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
}
