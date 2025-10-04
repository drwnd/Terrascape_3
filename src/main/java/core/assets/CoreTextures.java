package core.assets;

import core.assets.identifiers.TextureIdentifier;

public enum CoreTextures implements TextureIdentifier {

    GUI_ELEMENT_BACKGROUND("assets/textures/GuiElementBackground.png"),
    TOGGLE_ACTIVATED("assets/textures/ToggleActivated.png"),
    TOGGLE_DEACTIVATED("assets/textures/ToggleDeactivated.png"),
    OVERLAY("assets/textures/InventoryOverlay.png");


    CoreTextures(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String filepath() {
        return filepath;
    }

    private final String filepath;
}
