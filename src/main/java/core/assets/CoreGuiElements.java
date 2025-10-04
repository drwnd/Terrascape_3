package core.assets;

import core.assets.identifiers.GuiElementIdentifier;

public enum CoreGuiElements implements GuiElementIdentifier {

    QUAD(new float[]{0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 0}, new float[]{0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1});


    CoreGuiElements(float[] vertices, float[] textureCoordinates) {
        this.vertices = vertices;
        this.textureCoordinates = textureCoordinates;
    }

    @Override
    public float[] vertices() {
        return vertices;
    }

    @Override
    public float[] textureCoordinates() {
        return textureCoordinates;
    }

    private final float[] vertices, textureCoordinates;
}
