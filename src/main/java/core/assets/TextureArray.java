package core.assets;

import org.lwjgl.opengl.GL46;

public final class TextureArray extends Asset {

    public TextureArray(ObjectGenerator generator) {
        this.id = generator.generateObject();
    }

    public int getID() {
        return id;
    }

    @Override
    public void delete() {
        GL46.glDeleteTextures(id);
    }

    private final int id;
}
