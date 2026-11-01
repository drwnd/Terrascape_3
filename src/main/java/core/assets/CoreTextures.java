package core.assets;

import core.assets.identifiers.AssetIdentifier;
import core.utils.MainThread;

import java.nio.file.Path;

public enum CoreTextures implements AssetIdentifier<Texture2D> {

    GUI_ELEMENT_BACKGROUND("GuiElementBackground.png"),
    TOGGLE_ACTIVATED("ToggleActivated.png"),
    TOGGLE_DEACTIVATED("ToggleDeactivated.png"),
    OVERLAY("InventoryOverlay.png"),
    CURSOR_INDICATOR("CursorIndicator.png"),
    UP_ARROW("UpArrow.png"),
    DOWN_ARROW("DownArrow.png"),
    SLIDER_ICON("SliderIcon.png");


    CoreTextures(String fileName) {
        this.fileName = fileName;
    }

    @Override
    @MainThread
    public Texture2D generateAsset() {
        Path filepath = AssetManager.getAssetFilepath(Path.of("textures", fileName));
        return AssetLoader.loadTexture2D(filepath);
    }

    private final String fileName;
}
