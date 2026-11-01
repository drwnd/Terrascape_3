package core.assets;

public record GuiElementData(float[][] floatAttributes, int[][] intAttributes, int[] attributeSizes, int[] indices) {

    public int vertexCount() {
        return indices.length;
    }

    public void verifyCorrectness() {
        if (intAttributes() == null || floatAttributes() == null || attributeSizes() == null || indices() == null)
            throw new NullPointerException();

        if (floatAttributes.length + intAttributes.length != attributeSizes.length)
            throw new IllegalArgumentException("Specify same number of attributes and attribute sizes");

        int definedVerticesCount;
        if (floatAttributes.length == 0 && intAttributes.length == 0) definedVerticesCount = 0;
        else definedVerticesCount = (floatAttributes.length == 0 ? intAttributes[0].length : floatAttributes[0].length) / attributeSizes[0];

        for (int attributeSize : attributeSizes) if (attributeSize <= 0) throw new IllegalArgumentException("Sizes must be strictly positive integers");

        for (int index = 0; index < floatAttributes.length; index++) {
            float[] attribute = floatAttributes[index];
            if (attribute == null) throw new IllegalArgumentException("An attribute cannot be null");
            if (attribute.length / attributeSizes[index] != definedVerticesCount || attribute.length % attributeSizes[index] != 0)
                throw new IllegalArgumentException("Inconsistent vertex count");
        }

        for (int index = 0; index < intAttributes.length; index++) {
            int[] attribute = intAttributes[index];
            if (attribute == null) throw new IllegalArgumentException("An attribute cannot be null");
            if (attribute.length / attributeSizes[floatAttributes.length + index] != definedVerticesCount || attribute.length % attributeSizes[floatAttributes.length + index] != 0)
                throw new IllegalArgumentException("Inconsistent vertex count");
        }
    }
}
