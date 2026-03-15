package core.assets;

import static org.lwjgl.opengl.GL46.*;

public record TextureArray(int id, int[] textureSizes, int maxTextureSize) implements Asset {

    public TextureArray(int id, int[] textureSizes) {
        this(id, textureSizes, getMax(textureSizes));
    }

    @Override
    public void delete() {
        glDeleteTextures(id);
    }

    private static int getMax(int[] textureSizes) {
        int maxTextureSize = 0;
        for (int textureSize : textureSizes) maxTextureSize = Math.max(maxTextureSize, textureSize);
        return maxTextureSize;
    }
}
