package core.assets;

import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public record Texture2D(int id, int target, int width, int height) implements Texture {

    public Texture2D(int id) {
        this(id, GL_TEXTURE_2D, 0, 0);
    }

    public Texture2D(int id, int target) {
        this(id, target, 0, 0);
    }

    @Override
    @MainThread
    public void delete() {
        glDeleteTextures(id);
    }
}
