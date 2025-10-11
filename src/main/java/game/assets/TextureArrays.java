package game.assets;

import core.assets.AssetManager;
import core.assets.ObjectGenerator;
import core.assets.identifiers.TextureArrayIdentifier;
import game.player.rendering.ObjectLoader;

public enum TextureArrays implements TextureArrayIdentifier {

    MATERIALS(() -> ObjectLoader.generateAtlasTextureArray(AssetManager.get(Textures.MATERIALS))),
    PROPERTIES(() -> ObjectLoader.generateAtlasTextureArray(AssetManager.get(Textures.PROPERTIES)));

    TextureArrays(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
