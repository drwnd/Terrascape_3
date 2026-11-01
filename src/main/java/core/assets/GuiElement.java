package core.assets;

import core.utils.MainThread;

import static org.lwjgl.opengl.GL46.*;

public record GuiElement(int vao, int vertexCount) implements Asset {

    @Override
    @MainThread
    public void delete() {
        glDeleteVertexArrays(vao);
    }
}
