package game.assets;

import core.assets.identifiers.TextureIdentifier;

public enum Textures implements TextureIdentifier {

    DAY_SKY("DaySky.png"),
    NIGHT_SKY("NightSky.png"),
    CROSSHAIR("CrossHair.png"),
    HOTBAR("HotBar.png"),
    HOTBAR_SELECTION_INDICATOR("HotBarSelectionIndicator.png"),
    RELOAD_ICON("Reload.png");

    /**
     * Constructs a texture identifier with a file name.
     *
     * @param fileName the name of the texture file
     */
    Textures(String fileName) {
        this.filepath = fileName;
    }

    @Override
    public String fileName() {
        return filepath;
    }

    private final String filepath;
}
