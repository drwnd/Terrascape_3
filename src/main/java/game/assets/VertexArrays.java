package game.assets;

import core.assets.ObjectGenerator;
import core.assets.identifiers.VertexArrayIdentifier;

import game.player.rendering.ObjectLoader;
import core.utils.MainThread;

public enum VertexArrays implements VertexArrayIdentifier {

    SKYBOX(ObjectLoader::generateSkyboxVertexArray);

    @MainThread
    VertexArrays(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    @MainThread
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
