package core.assets;

import static org.lwjgl.opengl.GL46.*;

public final class Texture extends Asset {

    public Texture(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    public Texture(int id) {
        this.id = id;
        width = height = 0;
    }

    public int getID() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void delete() {
        glDeleteTextures(id);
    }

    private final int id;
    private final int width, height;
}
