package game.player.rendering;

import core.utils.IntArrayList;
import core.utils.Vector3l;

import static org.lwjgl.opengl.GL46.*;

import static game.utils.Constants.*;

/**
 * Performs transparent model.
 *
 * @param totalX X coordinate in local block coordinates
 * @param totalY Y coordinate in local block coordinates
 * @param totalZ Z coordinate in local block coordinates
 * @param LOD parameter
 * @param bufferOrStart parameter
 * @param transparentVertexCount parameter
 * @param glassVertexCount parameter
 * @param index X coordinate in local block coordinates
 * @return result
 */
public record TransparentModel(long totalX, long totalY, long totalZ, int LOD, int bufferOrStart, int transparentVertexCount, int glassVertexCount, int index) {

    public TransparentModel(Vector3l position, int transparentVertexCount, int glassVertexCount, int bufferOrStart, int lod) {
        this(position.x << lod, position.y << lod, position.z << lod,
                lod, bufferOrStart, transparentVertexCount, glassVertexCount,
                (bufferOrStart >> 2) * MeshGenerator.VERTICES_PER_QUAD / MeshGenerator.INTS_PER_VERTEX);
    }

    public void addDataWithOcclusionCulling(IntArrayList transparentCommands, IntArrayList glassCommands) {
        transparentCommands.add(isTransparentEmpty() ? 0 : transparentVertexCount);
        transparentCommands.add(0);
        transparentCommands.add(isTransparentEmpty() ? 0 : index);
        transparentCommands.add(0);

        glassCommands.add(isGlassEmpty() ? 0 : glassVertexCount);
        glassCommands.add(0);
        glassCommands.add(isGlassEmpty() ? 0 : index + transparentVertexCount);
        glassCommands.add(0);
    }

    public void addDataWithoutOcclusionCulling(IntArrayList transparentCommands, IntArrayList glassCommands) {
        if (!isTransparentEmpty()) addTransparentData(transparentCommands);
        if (!isGlassEmpty()) addGlassData(glassCommands);
    }

    public void addGlassData(IntArrayList glassCommands) {
        glassCommands.add(glassVertexCount);
        glassCommands.add(1);
        glassCommands.add(index + transparentVertexCount);
        glassCommands.add(0);
    }

    public void addTransparentData(IntArrayList transparentCommands) {
        transparentCommands.add(transparentVertexCount);
        transparentCommands.add(1);
        transparentCommands.add(index);
        transparentCommands.add(0);
    }

    public boolean isTransparentEmpty() {
        return transparentVertexCount == 0;
    }

    public boolean isGlassEmpty() {
        return glassVertexCount == 0;
    }

    public boolean isEmpty() {
        return isTransparentEmpty() && isGlassEmpty();
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
}

