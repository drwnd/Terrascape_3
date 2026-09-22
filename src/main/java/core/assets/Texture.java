package core.assets;

import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public interface Texture extends Asset {

    int id();

    int target();

    @Override
    @MainThread
    default void delete() {
        glDeleteTextures(id());
    }

    @MainThread
    default void bind() {
        glBindTexture(target(), id());
    }
}
