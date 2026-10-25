package core.assets.identifiers;

import core.assets.AssetLoader;
import core.assets.GuiElement;
import core.assets.GuiElementData;
import core.utils.MainThread;

public interface GuiElementIdentifier extends AssetIdentifier<GuiElement> {

    GuiElementData getData();

    @Override
    @MainThread
    default GuiElement generateAsset() {
        return AssetLoader.loadGuiElement(getData());
    }
}
