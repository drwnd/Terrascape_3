package core.assets.identifiers;

import game.utils.Constants;

public enum GuiElementIdentifier implements AssetIdentifier {

    QUAD(Constants.QUAD_VERTICES, Constants.QUAD_TEXTURE_COORDINATES);

    GuiElementIdentifier(float[] vertices, float[] textureCoordinates) {
        this.vertices = vertices;
        this.textureCoordinates = textureCoordinates;
    }

    public final float[] vertices, textureCoordinates;
}
