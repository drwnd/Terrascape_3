package game.assets;

import core.assets.identifiers.TextureIdentifier;

public enum Textures implements TextureIdentifier {

    DAY_SKY("DaySky.png"),
    NIGHT_SKY("NightSky.png"),
    CROSSHAIR("CrossHair.png"),
    HOTBAR("HotBar.png"),
    HOTBAR_SELECTION_INDICATOR("HotBarSelectionIndicator.png");

    Textures(String fileName) {
        this.filepath = fileName;
    }

    @Override
    public String fileName() {
        return filepath;
    }

    private final String filepath;
}
