package game.assets;

import core.assets.identifiers.TextureArrayIdentifier;
import core.utils.FileIndexSet;

import game.server.material.Materials;

public enum TextureArrays implements TextureArrayIdentifier {

    MATERIALS("albedo", new FileIndexSet(Materials.values(), ".png")),
    PROPERTIES("properties", new FileIndexSet(Materials.values(), ".png"));

    TextureArrays(String folderName, FileIndexSet indexSet) {
        this.folderName = folderName;
        this.indexSet = indexSet;
    }

    @Override
    public String folderName() {
        return folderName;
    }

    @Override
    public FileIndexSet indexSet() {
        return indexSet;
    }

    private final String folderName;
    private final FileIndexSet indexSet;
}
