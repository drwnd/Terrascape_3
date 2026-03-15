package core.assets;

import static org.lwjgl.opengl.GL46.*;

public record Texture(int id, int width, int height) implements Asset {

    public Texture(int id) {
        this(id, 0, 0);
    }

    @Override
    public void delete() {
        glDeleteTextures(id);
    }
}
