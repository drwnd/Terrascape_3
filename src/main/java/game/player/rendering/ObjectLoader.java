package game.player.rendering;

import core.assets.AssetLoader;
import core.rendering_api.shaders.TextShader;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static game.utils.Constants.*;

public final class ObjectLoader {

    public static OpaqueModel loadOpaqueModel(Mesh mesh) {
        Vector3i position = mesh.getWorldCoordinate();
        if (mesh.opaqueVertices().length == 0) return new OpaqueModel(position, null, 0, mesh.lod());
        int vertexBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(vertexBuffer, mesh.opaqueVertices(), GL46.GL_STATIC_DRAW);
        return new OpaqueModel(position, mesh.vertexCounts(), vertexBuffer, mesh.lod());
    }

    public static TransparentModel loadTransparentModel(Mesh mesh) {
        Vector3i position = mesh.getWorldCoordinate();
        if (mesh.waterVertexCount() + mesh.glassVertexCount() == 0) return new TransparentModel(position, 0, 0, 0, mesh.lod());
        int vertexBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(vertexBuffer, mesh.transparentVertices(), GL46.GL_STATIC_DRAW);
        return new TransparentModel(position, mesh.waterVertexCount(), mesh.glassVertexCount(), vertexBuffer, mesh.lod());
    }

    public static int generateModelIndexBuffer() {
        int[] indices = new int[393216];
        int index = 0;
        for (int i = 0; i < indices.length; i += 6) {
            indices[i] = index;
            indices[i + 1] = index + 1;
            indices[i + 2] = index + 2;
            indices[i + 3] = index + 3;
            indices[i + 4] = index + 2;
            indices[i + 5] = index + 1;
            index += 4;
        }
        int id = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, id);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, indices, GL46.GL_STATIC_DRAW);

        return id;
    }

    public static int generateTextRowVertexArray() {
        int vao = AssetLoader.createVAO();

        final int offsetX = 128;
        final int offsetY = 256;

        int[] textData = new int[TextShader.MAX_TEXT_LENGTH * 4];
        for (int i = 0; i < textData.length; i += 4) {
            textData[i] = i >> 2;
            textData[i + 1] = i >> 2 | offsetX;
            textData[i + 2] = i >> 2 | offsetY;
            textData[i + 3] = i >> 2 | offsetX | offsetY;
        }
        int vbo = AssetLoader.storeDateInAttributeList(textData);

        GL46.glBindVertexArray(0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo);
        GL46.glDeleteBuffers(vbo);

        return vao;
    }

    public static int generateSkyboxVertexArray() {
        int vao = AssetLoader.createVAO();
        AssetLoader.storeIndicesInBuffer(SKY_BOX_INDICES);
        AssetLoader.storeDateInAttributeList(0, 3, SKY_BOX_VERTICES);
        AssetLoader.storeDateInAttributeList(1, 2, SKY_BOX_TEXTURE_COORDINATES);
        return vao;
    }

    public static int createTexture2D(int internalFormat, int width, int height, int format, int type, int sampling) {
        int texture = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, texture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, sampling);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, sampling);
        return texture;
    }
}
