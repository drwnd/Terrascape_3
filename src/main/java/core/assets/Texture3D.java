package core.assets;

import static org.lwjgl.opengl.GL46.*;

public record Texture3D(int id, int target, int width, int height, int depth) implements Texture {

    public Texture3D(int id) {
        this(id, GL_TEXTURE_3D, 0, 0, 0);
    }

    public Texture3D(int id, int target) {
        this(id, target, 0, 0, 0);
    }
}
