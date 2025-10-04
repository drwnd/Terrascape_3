package game.assets;

import core.assets.identifiers.TextureIdentifier;

public enum Textures implements TextureIdentifier {

    MATERIALS("assets/textures/atlas256.png"),
    DAY_SKY("assets/textures/82984-skybox-blue-atmosphere-sky-space-hd-image-free-png.png"),
    NIGHT_SKY("assets/textures/706c5e1da58f47ad6e18145165caf55d.png"),
    CROSSHAIR("assets/textures/CrossHair.png"),
    HOTBAR("assets/textures/HotBar.png"),
    HOTBAR_SELECTION_INDICATOR("assets/textures/HotBarSelectionIndicator.png"),
    PROPERTIES("assets/textures/properties256.png");

    Textures(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String filepath() {
        return filepath;
    }

    private final String filepath;
}
