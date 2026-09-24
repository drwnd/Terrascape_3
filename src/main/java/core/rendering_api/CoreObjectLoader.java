package core.rendering_api;

import core.assets.Texture;
import core.assets.Texture2D;
import core.assets.TextureArray;
import core.utils.MainThread;

import static org.lwjgl.opengl.GL43.*;

public final class CoreObjectLoader {

    private CoreObjectLoader() {

    }

    @MainThread
    public static TextureArray generateTextureArray(Texture2D[] textures) {
        int textureSize = 0;
        for (Texture2D texture : textures) textureSize = Math.max(textureSize, texture.width());

        int textureArray = createTexture2DArray(GL_RGBA8, textureSize, textureSize, textures.length, GL_RGBA, GL_FLOAT, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        for (int index = 0; index < textures.length; index++) {
            Texture2D texture = textures[index];
            glCopyImageSubData(texture.id(), texture.target(), 0, 0, 0, 0,
                    textureArray, GL_TEXTURE_2D_ARRAY, 0, 0, 0, index,
                    texture.width(), texture.width(), 1);
        }

        int[] textureSizes = new int[textures.length];
        for (int index = 0; index < textures.length; index++)
            textureSizes[index] = textures[index].width();

        for (Texture texture : textures) texture.delete();
        return new TextureArray(textureArray, textureSizes);
    }

    @MainThread
    public static int createTexture2D(int internalFormat, int width, int height, int format, int type, int sampling) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, sampling);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, sampling);
        return texture;
    }

    @MainThread
    public static int createTexture2DArray(int internalFormat, int width, int height, int depth, int format, int type, int sampling) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, texture);
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, internalFormat, width, height, depth, 0, format, type, 0);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, sampling);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, sampling);
        return texture;
    }
}
