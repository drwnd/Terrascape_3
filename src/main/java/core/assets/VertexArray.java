package core.assets;

import static org.lwjgl.opengl.GL46.*;

public record VertexArray(int id) implements Asset {

    public VertexArray(ObjectGenerator generator) {
        this(generator.generateObject());
    }

    @Override
    public void delete() {
        glDeleteVertexArrays(id);
    }
}

