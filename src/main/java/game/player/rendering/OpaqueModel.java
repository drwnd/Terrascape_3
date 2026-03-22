package game.player.rendering;

import core.utils.IntArrayList;

import core.utils.Vector3l;

import game.utils.Utils;

import static org.lwjgl.opengl.GL46.*;

import static game.utils.Constants.*;

public record OpaqueModel(long totalX, long totalY, long totalZ, int LOD, int bufferOrStart, int[] vertexCounts, int[] toRenderVertexCounts, int[] indices) {

    public static final int FACE_COUNT = 7;

    public OpaqueModel(Vector3l position, int[] vertexCounts, int bufferOrStart, int lod, boolean isBuffer) {
        this(position.x << lod, position.y << lod, position.z << lod,
                lod, bufferOrStart, vertexCounts, new int[FACE_COUNT],
                getIndices(vertexCounts, bufferOrStart, isBuffer));
    }

    public void addDataWithOcclusionCulling(IntArrayList commands, long cameraChunkX, long cameraChunkY, long cameraChunkZ, boolean isLodBorderChunk) {
        long modelChunkX = chunkX();
        long modelChunkY = chunkY();
        long modelChunkZ = chunkZ();
        boolean notNull = !isEmpty();

        addData(commands, notNull && isVisibleGE(cameraChunkZ, modelChunkZ), NORTH);
        addData(commands, notNull && isVisibleGE(cameraChunkY, modelChunkY), TOP);
        addData(commands, notNull && isVisibleGE(cameraChunkX, modelChunkX), WEST);
        addData(commands, notNull && isVisibleLE(cameraChunkZ, modelChunkZ), SOUTH);
        addData(commands, notNull && isVisibleLE(cameraChunkY, modelChunkY), BOTTOM);
        addData(commands, notNull && isVisibleLE(cameraChunkX, modelChunkX), EAST);
        addData(commands, notNull && isLodBorderChunk, 6);
    }

    public void addDataWithoutOcclusionCulling(IntArrayList commands, long cameraChunkX, long cameraChunkY, long cameraChunkZ, boolean isLodBorderChunk) {
        if (isEmpty()) return;
        long modelChunkX = chunkX();
        long modelChunkY = chunkY();
        long modelChunkZ = chunkZ();

        if (isVisibleGE(cameraChunkZ, modelChunkZ)) addData(commands, NORTH);
        if (isVisibleGE(cameraChunkY, modelChunkY)) addData(commands, TOP);
        if (isVisibleGE(cameraChunkX, modelChunkX)) addData(commands, WEST);
        if (isVisibleLE(cameraChunkZ, modelChunkZ)) addData(commands, SOUTH);
        if (isVisibleLE(cameraChunkY, modelChunkY)) addData(commands, BOTTOM);
        if (isVisibleLE(cameraChunkX, modelChunkX)) addData(commands, EAST);
        if (isLodBorderChunk) addData(commands, 6);
    }

    public boolean isEmpty() {
        return vertexCounts == null || indices == null;
    }

    public void delete() {
        glDeleteBuffers(bufferOrStart);
    }

    public long chunkX() {
        return totalX >>> CHUNK_SIZE_BITS + LOD;
    }

    public long chunkY() {
        return totalY >>> CHUNK_SIZE_BITS + LOD;
    }

    public long chunkZ() {
        return totalZ >>> CHUNK_SIZE_BITS + LOD;
    }

    public int vertexCountSum() {
        if (vertexCounts == null) return 0;
        int sum = 0;
        for (int vertexCount : vertexCounts) sum += vertexCount;
        return sum;
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

    private boolean isVisibleLE(long cameraChunk, long modelChunk) {
        return cameraChunk <= Utils.getWrappedChunkCoordinate(modelChunk, cameraChunk, LOD);
    }

    private boolean isVisibleGE(long cameraChunk, long modelChunk) {
        return cameraChunk >= Utils.getWrappedChunkCoordinate(modelChunk, cameraChunk, LOD);
    }

    private static int[] getIndices(int[] vertexCounts, int bufferOrStart, boolean isBuffer) {
        if (vertexCounts == null) return null;
        int[] indices = new int[FACE_COUNT];
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
