package game.entities.model;

import core.assets.AssetLoader;
import core.assets.GuiElementData;
import core.utils.FloatArrayList;
import core.utils.IntArrayList;

import game.player.rendering.MeshGenerator;

import org.joml.Vector3f;

public record ModelData(ModelBox[] boxes, ModelStructure[] structures) {

    static final int VERTICES_PER_FACE = 4;
    static final int VERTICES_PER_BOX = VERTICES_PER_FACE * 6;
    static final Vector3f[] FACE_POSITIONS = new Vector3f[]{
            new Vector3f(0, 0, 1), new Vector3f(0, 1, 1), new Vector3f(1, 0, 1), new Vector3f(1, 1, 1)
    };

    public ModelPart[] parts() {
        int boxesLength = boxes == null ? 0 : boxes.length;
        int structuresLength = structures == null ? 0 : structures.length;
        ModelPart[] parts = new ModelPart[boxesLength + structuresLength];
        if (boxes != null)
            System.arraycopy(boxes, 0, parts, 0, boxesLength);
        if (structures != null)
            System.arraycopy(structures, 0, parts, boxesLength, structuresLength);
        return parts;
    }

    public static GuiElementData toGuiElementData(ModelPart[] parts) {
        int length = parts.length;

        FloatArrayList vertices = new FloatArrayList(length * VERTICES_PER_BOX * 3);
        FloatArrayList textureCoordinates = new FloatArrayList(length * VERTICES_PER_BOX * 2);
        IntArrayList intData = new IntArrayList(length * VERTICES_PER_BOX * 1);
        MeshGenerator generator = new MeshGenerator();

        for (int index = 0; index < length; index++) parts[index].addMesh(vertices, textureCoordinates, intData, generator, index);

        return new GuiElementData(
                new float[][]{vertices.trimmed(), textureCoordinates.trimmed()},
                new int[][]{intData.trimmed()},
                new int[]{3, 2, 1}, AssetLoader.getQuadIndices(vertices.size() / 3 / AssetLoader.VERTICES_PER_QUAD));
    }
}
