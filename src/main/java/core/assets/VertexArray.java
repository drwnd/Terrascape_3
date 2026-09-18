package core.assets;

import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public record VertexArray(int id) implements Asset {

    @MainThread
    public VertexArray(ObjectGenerator generator) {
        this(generator.generateObject());
    }

    @Override
    @MainThread
    public void delete() {
        glDeleteVertexArrays(id);
    }
}

