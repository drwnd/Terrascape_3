package game.player.rendering;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static game.utils.Constants.CHUNK_SIZE_BITS;

public record TransparentModel(int totalX, int totalY, int totalZ, int LOD, int verticesBuffer, int waterVertexCount, int glassVertexCount) {

    public TransparentModel(Vector3i position, int waterVertexCount, int glassVertexCount, int verticesBuffer, int lod) {
        this(position.x << lod, position.y << lod, position.z << lod, lod, verticesBuffer, waterVertexCount, glassVertexCount);
    }

    public boolean containsWater() {
        return waterVertexCount != 0;
    }

    public boolean containsGlass() {
        return glassVertexCount != 0;
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
}

