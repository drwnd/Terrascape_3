package game.player.rendering;

import core.assets.AssetLoader;
import core.utils.Vector3l;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class ObjectLoader {

    public static OpaqueModel loadOpaqueModel(Mesh mesh) {
        Vector3l position = mesh.getWorldCoordinate();
        if (mesh.opaqueVertices().length == 0) return new OpaqueModel(position, null, 0, mesh.lod(), true);
        int vertexBuffer = glCreateBuffers();
        glNamedBufferData(vertexBuffer, mesh.opaqueVertices(), GL_STATIC_DRAW);
        return new OpaqueModel(position, mesh.vertexCounts(), vertexBuffer, mesh.lod(), true);
    }

    public static TransparentModel loadTransparentModel(Mesh mesh) {
        Vector3l position = mesh.getWorldCoordinate();
        if (mesh.transparentVertices().length == 0) return new TransparentModel(position, 0, 0, 0, mesh.lod());
        int vertexBuffer = glCreateBuffers();
        glNamedBufferData(vertexBuffer, mesh.transparentVertices(), GL_STATIC_DRAW);
        return new TransparentModel(position, mesh.waterVertexCount(), mesh.glassVertexCount(), vertexBuffer, mesh.lod());
    }

    public static OpaqueModel loadCombinedModel(Mesh mesh) {
        Vector3l position = mesh.getWorldCoordinate();
        if (mesh.opaqueVertices().length == 0 && mesh.transparentVertices().length == 0)
            return new OpaqueModel(position, null, 0, mesh.lod(), true);
        int vertexBuffer = glCreateBuffers();

        mesh.vertexCounts()[6] = mesh.waterVertexCount() + mesh.glassVertexCount();
        int[] vertices = new int[mesh.opaqueVertices().length + mesh.transparentVertices().length];
        System.arraycopy(mesh.opaqueVertices(), 0, vertices, 0, mesh.opaqueVertices().length);
        System.arraycopy(mesh.transparentVertices(), 0, vertices, mesh.opaqueVertices().length, mesh.transparentVertices().length);

        glNamedBufferData(vertexBuffer, vertices, GL_STATIC_DRAW);
        return new OpaqueModel(position, mesh.vertexCounts(), vertexBuffer, mesh.lod(), true);
    }

    public static int generateSkyboxVertexArray() {
        int vao = AssetLoader.createVAO();
        AssetLoader.storeIndicesInBuffer(SKY_BOX_INDICES);
        AssetLoader.storeDateInAttributeList(0, 3, SKY_BOX_VERTICES);
        AssetLoader.storeDateInAttributeList(1, 2, SKY_BOX_TEXTURE_COORDINATES);
        return vao;
    }
}
