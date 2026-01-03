package core.assets;

import static org.lwjgl.opengl.GL46.*;

public final class TextureArray extends Asset {

    public TextureArray(int id, int[] textureSizes) {
        this.id = id;
        this.textureSizes = textureSizes;

        int maxTextureSize = 0;
        for (int textureSize : textureSizes) maxTextureSize = Math.max(maxTextureSize, textureSize);
        this.maxTextureSize = maxTextureSize;
    }

    public int getID() {
        return id;
    }

    public int[] getTextureSizes() {
        return textureSizes;
    }

    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    @Override
    public void delete() {
        glDeleteTextures(id);
    }

    private final int id;
    private final int[] textureSizes;
    private final int maxTextureSize;
}
