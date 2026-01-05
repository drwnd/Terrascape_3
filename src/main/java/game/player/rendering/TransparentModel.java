package game.player.rendering;

import core.utils.IntArrayList;
import org.joml.Vector3i;

import static org.lwjgl.opengl.GL46.*;

import static game.utils.Constants.*;

public record TransparentModel(int totalX, int totalY, int totalZ, int LOD, int bufferOrStart, int waterVertexCount, int glassVertexCount, int index) {

    public TransparentModel(Vector3i position, int waterVertexCount, int glassVertexCount, int bufferOrStart, int lod) {
        this(position.x << lod, position.y << lod, position.z << lod,
                lod, bufferOrStart, waterVertexCount, glassVertexCount,
                (bufferOrStart >> 2) * MeshGenerator.VERTICES_PER_QUAD / MeshGenerator.INTS_PER_VERTEX);
    }

    public void addDataWithOcclusionCulling(IntArrayList waterCommands, IntArrayList glassCommands) {
        waterCommands.add(isWaterEmpty() ? 0 : waterVertexCount);
        waterCommands.add(0);
        waterCommands.add(isWaterEmpty() ? 0 : index);
        waterCommands.add(0);

        glassCommands.add(isGlassEmpty() ? 0 : glassVertexCount);
        glassCommands.add(0);
        glassCommands.add(isGlassEmpty() ? 0 : index + waterVertexCount);
        glassCommands.add(0);
    }

    public void addDataWithoutOcclusionCulling(IntArrayList waterCommands, IntArrayList glassCommands) {
        if (!isWaterEmpty()) {
            waterCommands.add(waterVertexCount);
            waterCommands.add(1);
            waterCommands.add(index);
            waterCommands.add(0);
        }

        if (!isGlassEmpty()) {
            glassCommands.add(glassVertexCount);
            glassCommands.add(1);
            glassCommands.add(index + waterVertexCount);
            glassCommands.add(0);
        }
    }

    public boolean isWaterEmpty() {
        return waterVertexCount == 0;
    }

    public boolean isGlassEmpty() {
        return glassVertexCount == 0;
    }

    public boolean isEmpty() {
        return isWaterEmpty() && isGlassEmpty();
    }

    public void delete() {
        glDeleteBuffers(bufferOrStart);
    }

    public int chunkX() {
        return totalX >>> CHUNK_SIZE_BITS + LOD;
    }

    public int chunkY() {
        return totalY >>> CHUNK_SIZE_BITS + LOD;
    }

    public int chunkZ() {
        return totalZ >>> CHUNK_SIZE_BITS + LOD;
    }
}

