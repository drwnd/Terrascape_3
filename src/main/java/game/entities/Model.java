package game.entities;

import core.assets.*;
import core.utils.MainThread;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public record Model(GuiElement guiElement, ModelPart[] parts, Animator animator) implements Asset {

    @MainThread
    public Model(ModelData data, Animator animator) {
        this(AssetLoader.loadGuiElement(data.toGuiElementData()), data.parts(), animator);
    }

    @Override
    public void delete() {
        guiElement.delete();
    }

    public interface ModelPart {
        Vector3f position();
    }

    record ModelBox(Vector3i size, Vector3f center, Vector3f position, Vector2f textureCoordinate, boolean hasOuterLayer,
                            Vector2f outerTextureCoordinate) implements ModelPart {
    }

    record ModelStructure(Vector3f center, Vector3f position, String structureName) implements ModelPart {

    }
}
