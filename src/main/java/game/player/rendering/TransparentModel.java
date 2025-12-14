package game.player.rendering;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static game.utils.Constants.CHUNK_SIZE_BITS;

public record TransparentModel(int totalX, int totalY, int totalZ, int LOD, int bufferOrStart, int waterVertexCount, int glassVertexCount, int index) {

    public TransparentModel(Vector3i position, int waterVertexCount, int glassVertexCount, int bufferOrStart, int lod) {
        this(position.x << lod, position.y << lod, position.z << lod,
                lod, bufferOrStart, waterVertexCount, glassVertexCount,
                (bufferOrStart >> 2) * MeshGenerator.VERTICES_PER_QUAD / MeshGenerator.INTS_PER_VERTEX);
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
}

