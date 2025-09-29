package core.assets.identifiers;

import core.assets.ObjectGenerator;
import game.player.rendering.ObjectLoader;

public enum BufferIdentifier implements AssetIdentifier {

    MODEL_INDEX(ObjectLoader::generateModelIndexBuffer);

    BufferIdentifier(ObjectGenerator generator) {
        this.generator = generator;
    }

    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
