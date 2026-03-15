package core.assets;

import static org.lwjgl.opengl.GL46.*;

public record GuiElement(int vao, int vertexCount) implements Asset {

    @Override
    public void delete() {
        glDeleteVertexArrays(vao);
    }
}
