package game.assets;

import core.assets.ObjectGenerator;
import core.assets.identifiers.VertexArrayIdentifier;

import game.player.rendering.ObjectLoader;

public enum VertexArrays implements VertexArrayIdentifier {

    SKYBOX(ObjectLoader::generateSkyboxVertexArray);


    /**
     * Constructs a vertex array identifier with an object generator.
     *
     * @param generator the generator used to create the vertex array
     */
    VertexArrays(ObjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ObjectGenerator getGenerator() {
        return generator;
    }

    private final ObjectGenerator generator;
}
