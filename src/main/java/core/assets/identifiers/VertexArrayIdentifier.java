package core.assets.identifiers;

import core.assets.ObjectGenerator;
import game.player.rendering.ObjectLoader;

public enum VertexArrayIdentifier implements AssetIdentifier {

    TEXT_ROW(ObjectLoader::generateTextRowVertexArray),
    SKYBOX(ObjectLoader::generateSkyboxVertexArray);

    VertexArrayIdentifier(ObjectGenerator generator) {
        this.generator = generator;
    }

    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
