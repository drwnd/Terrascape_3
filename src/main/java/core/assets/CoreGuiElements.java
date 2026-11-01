package core.assets;

import core.assets.identifiers.GuiElementIdentifier;

public enum CoreGuiElements implements GuiElementIdentifier {

    QUAD(new float[]{0, 0, 0, 1, 1, 0, 1, 1}, new float[]{0, 1, 0, 0, 1, 1, 1, 0});

    private static final int ATTRIBUTE_SIZE = 2;

    CoreGuiElements(float[] vertices, float[] textureCoordinates) {
        this.vertices = vertices;
        this.textureCoordinates = textureCoordinates;
    }

    @Override
    public GuiElementData getData() {
        return new GuiElementData(
                new float[][]{vertices, textureCoordinates}, new int[0][], new int[]{ATTRIBUTE_SIZE, ATTRIBUTE_SIZE},
                AssetLoader.getQuadIndices(vertices.length / ATTRIBUTE_SIZE / AssetLoader.VERTICES_PER_QUAD));
    }

    private final float[] vertices, textureCoordinates;
}
