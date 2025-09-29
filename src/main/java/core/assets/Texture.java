package core.assets;

import org.lwjgl.opengl.GL46;

public final class Texture extends Asset {

    public Texture(int id) {
        this.id = id;
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
