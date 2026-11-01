package game.entities.model;

import core.assets.*;
import core.utils.MainThread;

import java.util.Arrays;

public record Model(GuiElement guiElement, ModelPart[] parts) implements Asset {

    @MainThread
    public Model(ModelData data) {
        this(AssetLoader.loadGuiElement(ModelData.toGuiElementData(data.parts())), data.parts());
    }

    public Model(GuiElement guiElement, ModelPart[] parts) {
        this.guiElement = guiElement;
        this.parts = parts;
        if (!areParentIndicesValid(parts)) throw new IllegalStateException("The models parent-graph is not valid");
    }

    @Override
    @MainThread
    public void delete() {
        guiElement.delete();
    }


    private static boolean areParentIndicesValid(ModelPart[] parts) {
        for (ModelPart part : parts) if (part == null) return false;
        boolean[] touched = new boolean[parts.length];

        for (int index = 0; index < parts.length; index++) {
            Arrays.fill(touched, false);
            ModelPart part = parts[index];

            int currentIndex = index;
            while (part.parentIndex() != null) {
                if (part.parentIndex() >= parts.length || touched[currentIndex]) return false;
                touched[currentIndex] = true;
                currentIndex = part.parentIndex();
                part = parts[currentIndex];
            }
        }
        return true;
    }
}
