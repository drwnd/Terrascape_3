package core.assets;

import static org.lwjgl.opengl.GL46.*;

public record Buffer(int id) implements Asset {

    public Buffer(ObjectGenerator generator) {
        this(generator.generateObject());
    }

    @Override
    public void delete() {
        glDeleteBuffers(id);
    }
}
