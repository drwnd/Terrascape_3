package core.assets;

import core.assets.identifiers.TextureIdentifier;

public enum CoreTextures implements TextureIdentifier {

    GUI_ELEMENT_BACKGROUND("GuiElementBackground.png"),
    TOGGLE_ACTIVATED("ToggleActivated.png"),
    TOGGLE_DEACTIVATED("ToggleDeactivated.png"),
    OVERLAY("InventoryOverlay.png"),
    CURSOR_INDICATOR("CursorIndicator.png");


    CoreTextures(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String fileName() {
        return fileName;
    }

    private final String fileName;
}
