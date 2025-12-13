package core.assets;

import core.assets.identifiers.BufferIdentifier;

public enum CoreBuffers implements BufferIdentifier {

    TEXT_ELEMENT_ARRAY_BUFFER(() -> AssetLoader.generateModelIndexBuffer(128));


    CoreBuffers(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
