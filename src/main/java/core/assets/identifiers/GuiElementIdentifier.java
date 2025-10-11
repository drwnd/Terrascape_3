package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.assets.AssetLoader;
import core.assets.GuiElement;

public interface GuiElementIdentifier extends AssetIdentifier<GuiElement> {

    float[] vertices();

    float[] textureCoordinates();

    @Override
    default AssetGenerator<GuiElement> getAssetGenerator() {
        return () -> AssetLoader.loadGuiElement(this);
    }
}
