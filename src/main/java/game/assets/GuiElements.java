package game.assets;

import core.assets.identifiers.GuiElementIdentifier;
import game.utils.Constants;

public enum GuiElements implements GuiElementIdentifier {

    QUAD(Constants.QUAD_VERTICES, Constants.QUAD_TEXTURE_COORDINATES);

    GuiElements(float[] vertices, float[] textureCoordinates) {
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
