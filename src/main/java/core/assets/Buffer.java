package core.assets;

import static org.lwjgl.opengl.GL46.*;

public final class Buffer extends Asset {

    public Buffer(ObjectGenerator generator) {
        this.id = generator.generateObject();
    }

    @Override
    public void delete() {
        glDeleteBuffers(id);
    }

    public int getID() {
        return id;
    }

    private final int id;
}

