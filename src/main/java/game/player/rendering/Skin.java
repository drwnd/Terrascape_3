package game.player.rendering;

import core.assets.AssetLoader;
import core.assets.Texture;
import core.assets.identifiers.TextureIdentifier;
import core.settings.optionSettings.Option;
import core.utils.FileManager;
import core.utils.MainThread;

import java.io.File;
import java.nio.file.Path;

public final class Skin implements Option, TextureIdentifier {

    public Skin(String skinName) {
        this(Path.of("assets", "skins", skinName).toFile());
    }

    private Skin(File skinFile) {
        this.skinFile = skinFile;
    }

    @Override
    public Option next() {
        File[] skins = FileManager.getSiblings(skinFile);
        int index = (FileManager.indexOf(skinFile, skins) + 1) % skins.length;
        return new Skin(skins[index]);
    }

    @Override
    public Option previous() {
        File[] skins = FileManager.getSiblings(skinFile);
        int index = (FileManager.indexOf(skinFile, skins) - 1 + skins.length) % skins.length;
        return new Skin(skins[index]);
    }

    @Override
    public Option value(String name) {
        return new Skin(name);
    }

    @Override
    public int ordinal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String name() {
        return skinFile.getName();
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public String fileName() {
        return skinFile.getName();
    }

    @Override
    @MainThread
    public Texture generateAsset() {
        Path filepath = Path.of("assets", "skins", fileName());
        return AssetLoader.loadTexture2D(filepath);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Skin)) return false;
        return skinFile.getName().equalsIgnoreCase(((Skin) obj).skinFile.getName());
    }

    @Override
    public int hashCode() {
        return skinFile.getName().hashCode();
    }

    private final File skinFile;
}
