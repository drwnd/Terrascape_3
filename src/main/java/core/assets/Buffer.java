package core.assets;

import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public record Buffer(int id) implements Asset {

    @MainThread
    public Buffer(ObjectGenerator generator) {
        this(generator.generateObject());
    }

    @Override
    @MainThread
    public void delete() {
        glDeleteBuffers(id);
    }
}
