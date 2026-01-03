package game.assets;

import core.assets.TextureArrayGenerator;
import core.assets.identifiers.TextureArrayIdentifier;

import game.player.rendering.ObjectLoader;

public enum TextureArrays implements TextureArrayIdentifier {

    MATERIALS(() -> ObjectLoader.generateTextureArray("assets/textures/albedo")),
    PROPERTIES(() -> ObjectLoader.generateTextureArray("assets/textures/properties"));

    TextureArrays(TextureArrayGenerator generator) {
        this.generator = generator;
    }

    @Override
    public TextureArrayGenerator getGenerator() {
        return generator;
    }

    private final TextureArrayGenerator generator;
}
