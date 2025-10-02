package game.assets;

import core.assets.ObjectGenerator;
import core.assets.identifiers.BufferIdentifier;
import game.player.rendering.ObjectLoader;

public enum Buffers implements BufferIdentifier {

    MODEL_INDEX(ObjectLoader::generateModelIndexBuffer);

    Buffers(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
