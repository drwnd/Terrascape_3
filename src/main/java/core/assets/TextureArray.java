package core.assets;

import static org.lwjgl.opengl.GL46.*;

public final class TextureArray extends Asset {

    public TextureArray(ObjectGenerator generator) {
        this.id = generator.generateObject();
    }

    public int getID() {
        return id;
    }

    @Override
    public void delete() {
        glDeleteTextures(id);
    }

    private final int id;
}
