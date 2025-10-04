package core.assets;

import core.assets.identifiers.VertexArrayIdentifier;

public enum CoreVertexArrays implements VertexArrayIdentifier {

    TEXT_ROW(AssetLoader::generateTextRowVertexArray);


    CoreVertexArrays(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;

}
