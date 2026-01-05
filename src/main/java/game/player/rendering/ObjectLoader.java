package game.player.rendering;

import core.assets.AssetLoader;

import core.assets.Texture;
import core.assets.TextureArray;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class ObjectLoader {

    public static OpaqueModel loadOpaqueModel(Mesh mesh) {
        Vector3i position = mesh.getWorldCoordinate();
        if (mesh.opaqueVertices().length == 0) return new OpaqueModel(position, null, 0, mesh.lod(), true);
        int vertexBuffer = glCreateBuffers();
        glNamedBufferData(vertexBuffer, mesh.opaqueVertices(), GL_STATIC_DRAW);
        return new OpaqueModel(position, mesh.vertexCounts(), vertexBuffer, mesh.lod(), true);
    }

    public static TransparentModel loadTransparentModel(Mesh mesh) {
        Vector3i position = mesh.getWorldCoordinate();
        if (mesh.waterVertexCount() + mesh.glassVertexCount() == 0) return new TransparentModel(position, 0, 0, 0, mesh.lod());
        int vertexBuffer = glCreateBuffers();
        glNamedBufferData(vertexBuffer, mesh.transparentVertices(), GL_STATIC_DRAW);
        return new TransparentModel(position, mesh.waterVertexCount(), mesh.glassVertexCount(), vertexBuffer, mesh.lod());
    }

    public static int generateSkyboxVertexArray() {
        int vao = AssetLoader.createVAO();
        AssetLoader.storeIndicesInBuffer(SKY_BOX_INDICES);
        AssetLoader.storeDateInAttributeList(0, 3, SKY_BOX_VERTICES);
        AssetLoader.storeDateInAttributeList(1, 2, SKY_BOX_TEXTURE_COORDINATES);
        return vao;
    }

    public static TextureArray generateTextureArray(Texture[] textures) {
        int textureSize = 0;
        for (Texture texture : textures) textureSize = Math.max(textureSize, texture.getWidth());

        int textureArray = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureArray);
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, 4, GL_RGBA8, textureSize, textureSize, textures.length);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        for (int index = 0; index < textures.length; index++) {
            Texture texture = textures[index];
            glCopyImageSubData(texture.getID(), GL_TEXTURE_2D, 0, 0, 0, 0,
                    textureArray, GL_TEXTURE_2D_ARRAY, 0, 0, 0, index,
                    texture.getWidth(), texture.getWidth(), 1);
        }

        int[] textureSizes = new int[textures.length];
        for (int index = 0; index < textures.length; index++)
            textureSizes[index] = textures[index].getWidth();

        for (Texture texture : textures) glDeleteTextures(texture.getID());
        return new TextureArray(textureArray, textureSizes);
    }

    public static int createTexture2D(int internalFormat, int width, int height, int format, int type, int sampling) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, sampling);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, sampling);
        return texture;
    }
}
