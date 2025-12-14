package game.player.rendering;

import core.assets.AssetLoader;
import core.assets.Texture;

import game.server.material.Material;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import static game.utils.Constants.*;

public final class ObjectLoader {

    public static OpaqueModel loadOpaqueModel(Mesh mesh) {
        Vector3i position = mesh.getWorldCoordinate();
        if (mesh.opaqueVertices().length == 0) return new OpaqueModel(position, null, 0, mesh.lod(), true);
        int vertexBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(vertexBuffer, mesh.opaqueVertices(), GL46.GL_STATIC_DRAW);
        return new OpaqueModel(position, mesh.vertexCounts(), vertexBuffer, mesh.lod(), true);
    }

    public static TransparentModel loadTransparentModel(Mesh mesh) {
        Vector3i position = mesh.getWorldCoordinate();
        if (mesh.waterVertexCount() + mesh.glassVertexCount() == 0) return new TransparentModel(position, 0, 0, 0, mesh.lod());
        int vertexBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(vertexBuffer, mesh.transparentVertices(), GL46.GL_STATIC_DRAW);
        return new TransparentModel(position, mesh.waterVertexCount(), mesh.glassVertexCount(), vertexBuffer, mesh.lod());
    }

    public static int generateSkyboxVertexArray() {
        int vao = AssetLoader.createVAO();
        AssetLoader.storeIndicesInBuffer(SKY_BOX_INDICES);
        AssetLoader.storeDateInAttributeList(0, 3, SKY_BOX_VERTICES);
        AssetLoader.storeDateInAttributeList(1, 2, SKY_BOX_TEXTURE_COORDINATES);
        return vao;
    }

    public static int generateAtlasTextureArray(Texture atlas) {
        final int textureSize = 16;
        int atlasId = atlas.getID();

        int textureArray = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, textureArray);
        GL46.glTexStorage3D(GL46.GL_TEXTURE_2D_ARRAY, 4, GL46.GL_RGBA8, textureSize, textureSize, AMOUNT_OF_MATERIALS);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_WRAP_R, GL46.GL_CLAMP_TO_EDGE);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_EDGE);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D_ARRAY, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_EDGE);

        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            int textureCoordinate = Material.getTextureIndex((byte) material);
            int u = (textureCoordinate & 0xF) * textureSize;
            int v = (textureCoordinate >> 4 & 0xF) * textureSize;

            GL46.glCopyImageSubData(
                    atlasId, GL46.GL_TEXTURE_2D, 0, u, v, 0,
                    textureArray, GL46.GL_TEXTURE_2D_ARRAY, 0, 0, 0, material,
                    textureSize, textureSize, 1);
        }

        return textureArray;
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
