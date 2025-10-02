package game.assets;

import core.assets.AssetManager;
import core.assets.ObjectGenerator;
import core.assets.identifiers.TextureArrayIdentifier;
import game.player.rendering.ObjectLoader;

public enum TextureArrays implements TextureArrayIdentifier {

    MATERIALS(() -> ObjectLoader.generateAtlasTextureArray(AssetManager.getTexture(Textures.MATERIALS))),
    PROPERTIES(() -> ObjectLoader.generateAtlasTextureArray(AssetManager.getTexture(Textures.PROPERTIES)));

    TextureArrays(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
