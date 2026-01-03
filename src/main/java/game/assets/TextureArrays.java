package game.assets;

import core.assets.ObjectGenerator;
import core.assets.identifiers.TextureArrayIdentifier;

import game.player.rendering.ObjectLoader;

public enum TextureArrays implements TextureArrayIdentifier {

    MATERIALS(() -> ObjectLoader.generateTextureArray("assets/textures/albedo")),
    PROPERTIES(() -> ObjectLoader.generateTextureArray("assets/textures/properties"));

    TextureArrays(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
