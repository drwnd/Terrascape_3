package core.assets;

import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public record TextureArray(int id, int[] textureSizes, int maxTextureSize) implements Asset {

    public TextureArray(int id, int[] textureSizes) {
        this(id, textureSizes, getMax(textureSizes));
    }

    @Override
    @MainThread
    public void delete() {
        glDeleteTextures(id);
    }

    @MainThread
    public void bind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
    }

    private static int getMax(int[] textureSizes) {
        int maxTextureSize = 0;
        for (int textureSize : textureSizes) maxTextureSize = Math.max(maxTextureSize, textureSize);
        return maxTextureSize;
    }
}
