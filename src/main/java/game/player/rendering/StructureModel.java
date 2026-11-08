package game.player.rendering;

import core.assets.AssetLoader;
import core.assets.GuiElement;
import core.assets.GuiElementData;
import core.utils.MainThread;
import game.entities.model.*;
import game.server.generation.Structure;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL46.*;

public final class StructureModel {

    public StructureModel(BufferFormat format) {
        this.format = format;
    }

    @MainThread
    public void synchronizeModel(int size, int hash, Structure structure) {
        if (!valid || this.size != size || this.hash != hash) {
            if (buffer != -1) format.delete(buffer);
            this.size = size;
            this.hash = hash;
            valid = true;

            if (structure == null) {
                buffer = -1;
                vertexCount = 0;
                return;
            }

            switch (format) {
                case STRUCTURE -> {
                    Mesh mesh = new MeshGenerator().generateMesh(structure);
                    OpaqueModel opaqueModel = ObjectLoader.loadCombinedModel(mesh);
                    buffer = opaqueModel.bufferOrStart();
                    vertexCount = opaqueModel.vertexCountSum();
                }
                case MODEL -> {
                    Vector3f center = new Vector3f(structure.centerX(), structure.centerY(), structure.centerZ());
                    ModelPart part = new CustomModelStructure(center, null, structure, null, false);
                    GuiElementData guiElementData = ModelData.toGuiElementData(new ModelPart[]{part});
                    GuiElement guiElement = AssetLoader.loadGuiElement(guiElementData);
                    buffer = guiElement.vao();
                    vertexCount = guiElement.vertexCount();
                }
            }
        }
    }

    public int getBuffer() {
        return buffer;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getSize() {
        return size;
    }

    public void invalidate() {
        valid = false;
    }

    public boolean canRender() {
        return valid && buffer != -1;
    }

    public enum BufferFormat {
        STRUCTURE, MODEL;

        private void delete(int buffer) {
            switch (this) {
                case STRUCTURE -> glDeleteBuffers(buffer);
                case MODEL -> glDeleteVertexArrays(buffer);
            }
        }
    }

    private final BufferFormat format;
    private int buffer = -1, vertexCount;
    private boolean valid = false;
    private int size, hash;
}
