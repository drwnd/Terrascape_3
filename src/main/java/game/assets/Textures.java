package game.assets;

import core.assets.AssetLoader;
import core.assets.AssetManager;
import core.assets.Texture2D;
import core.assets.identifiers.AssetIdentifier;
import core.utils.MainThread;

import java.nio.file.Path;

public enum Textures implements AssetIdentifier<Texture2D> {

    DAY_SKY("DaySky.png"),
    NIGHT_SKY("NightSky.png"),
    CROSSHAIR("CrossHair.png"),
    HOTBAR("HotBar.png"),
    HOTBAR_SELECTION_INDICATOR("HotBarSelectionIndicator.png"),
    RELOAD_ICON("Reload.png");

    Textures(String fileName) {
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
