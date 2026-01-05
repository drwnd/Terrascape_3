package core.assets;

import core.assets.identifiers.TextureIdentifier;

public enum CoreTextures implements TextureIdentifier {

    GUI_ELEMENT_BACKGROUND("GuiElementBackground.png"),
    TOGGLE_ACTIVATED("ToggleActivated.png"),
    TOGGLE_DEACTIVATED("ToggleDeactivated.png"),
    OVERLAY("InventoryOverlay.png");


    CoreTextures(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String fileName() {
        return filepath;
    }

    private final String filepath;
}
