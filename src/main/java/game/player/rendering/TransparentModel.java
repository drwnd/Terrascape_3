package game.player.rendering;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

public record TransparentModel(int X, int Y, int Z, int LOD, int verticesBuffer, int waterVertexCount, int glassVertexCount) {

    public TransparentModel(Vector3i position, int waterVertexCount, int glassVertexCount, int verticesBuffer, int lod) {
        this(position.x, position.y, position.z, lod, verticesBuffer, waterVertexCount, glassVertexCount);
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
}

