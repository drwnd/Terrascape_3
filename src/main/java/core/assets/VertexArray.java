package core.assets;

import org.lwjgl.opengl.GL46;

public final class VertexArray extends Asset {

    public VertexArray(ObjectGenerator generator) {
        this.id = generator.generateObject();
    }

    @Override
    public void delete() {
        GL46.glDeleteVertexArrays(id);
    }

    public int getID() {
        return id;
    }

    private final int id;
}

