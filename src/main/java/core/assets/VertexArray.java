package core.assets;

import static org.lwjgl.opengl.GL46.*;

public final class VertexArray extends Asset {

    public VertexArray(ObjectGenerator generator) {
        this.id = generator.generateObject();
    }

    @Override
    public void delete() {
        glDeleteVertexArrays(id);
    }

    public int getID() {
        return id;
    }

    private final int id;
}

