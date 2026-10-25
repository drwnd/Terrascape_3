package game.entities;

import core.assets.AssetLoader;
import core.assets.AssetManager;
import core.assets.GuiElementData;
import game.assets.StructureIdentifier;
import game.player.rendering.Mesh;
import game.player.rendering.MeshGenerator;
import game.server.generation.Structure;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

import static game.utils.Constants.*;

public record ModelData(Model.ModelBox[] boxes, Model.ModelStructure[] structures) {

    public Model.ModelPart[] parts() {
        int boxesLength = boxes == null ? 0 : boxes.length;
        int structuresLength = structures == null ? 0 : structures.length;
        Model.ModelPart[] parts = new Model.ModelPart[boxesLength + structuresLength];
        if (boxes != null)
            System.arraycopy(boxes, 0, parts, 0, boxesLength);
        if (structures != null)
            System.arraycopy(structures, 0, parts, boxesLength, structuresLength);
        return parts;
    }

    public GuiElementData toGuiElementData() {
        int boxesLength = boxes == null ? 0 : boxes.length;
        int structuresLength = structures == null ? 0 : structures.length;

        ArrayList<Float> vertices = new ArrayList<>(boxesLength * VERTICES_PER_BOX * 3);
        ArrayList<Float> textureCoordinates = new ArrayList<>(boxesLength * VERTICES_PER_BOX * 2);
        ArrayList<Integer> intData = new ArrayList<>(boxesLength * VERTICES_PER_BOX * 1);
        MeshGenerator generator = new MeshGenerator();

        for (int index = 0; index < boxesLength; index++) {
            Model.ModelBox box = boxes[index];
            int amount = box.hasOuterLayer() ? VERTICES_PER_BOX * 2 : VERTICES_PER_BOX * 1;

            generateModelBox(vertices, textureCoordinates, box.size(), box.center(), false, box.textureCoordinate());
            if (box.hasOuterLayer()) generateModelBox(vertices, textureCoordinates, box.size(), box.center(), true, box.outerTextureCoordinate());

            for (int count = 0; count < amount; count++) intData.add((count / VERTICES_PER_FACE % 6) << 29 | index);
        }

        for (int index = 0; index < structuresLength; index++) {
            Model.ModelStructure modelStructure = structures[index];
            Structure structure = AssetManager.get(new StructureIdentifier(modelStructure.structureName()));

            Mesh mesh = generator.generateMesh(structure);
            int[] opaqueVertices = mesh.opaqueVertices();
            for (int vertexIndex = 0; vertexIndex < opaqueVertices.length; vertexIndex += MeshGenerator.INTS_PER_VERTEX) {
                int x = opaqueVertices[vertexIndex + 0];
                int y = opaqueVertices[vertexIndex + 1];
                int z = opaqueVertices[vertexIndex + 2];
                int textureData = opaqueVertices[vertexIndex + 3];
                generateModelFace(vertices, textureCoordinates, modelStructure.center(), x, y, z, textureData);

                int side = textureData >> 8 & 7;
                int material = textureData & 0xFF;
                for (int count = 0; count < VERTICES_PER_FACE; count++) intData.add(side << 29 | material << 21 | index + boxesLength);
            }
        }

        return new GuiElementData(
                new float[][]{toFloatArray(vertices), toFloatArray(textureCoordinates)},
                new int[][]{toIntArray(intData)},
                new int[]{3, 2, 1}, AssetLoader.getQuadIndices(vertices.size() / 3 / AssetLoader.VERTICES_PER_QUAD));
    }

    private static void generateModelFace(ArrayList<Float> vertices, ArrayList<Float> textureCoordinates, Vector3f center, int x, int y, int z, int textureData) {
        int side = textureData >> 8 & 7;

        float faceSize1 = (textureData >> 17 & 63) + 1;
        float faceSize2 = (textureData >> 11 & 63) + 1;
        Vector3f pos0 = getFacePosition(side, 0, faceSize1, faceSize2).sub(center).add(x, y, z);
        Vector3f pos1 = getFacePosition(side, 1, faceSize1, faceSize2).sub(center).add(x, y, z);
        Vector3f pos2 = getFacePosition(side, 2, faceSize1, faceSize2).sub(center).add(x, y, z);
        Vector3f pos3 = getFacePosition(side, 3, faceSize1, faceSize2).sub(center).add(x, y, z);

        vertices.addAll(List.of(
                pos0.x, pos0.y, pos0.z,
                pos1.x, pos1.y, pos1.z,
                pos2.x, pos2.y, pos2.z,
                pos3.x, pos3.y, pos3.z
        ));

        if (side == NORTH || side == TOP || side == EAST) {
            float temp = faceSize1;
            faceSize1 = faceSize2;
            faceSize2 = temp;
        }

        textureCoordinates.addAll(List.of(
                0.0F, 0.0F,
                faceSize1, 0.0F,
                0.0F, -faceSize2,
                faceSize1, -faceSize2
        ));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static Vector3f getFacePosition(int side, int currentVertexId, float faceSize1, float faceSize2) {
        Vector3f offset = FACE_POSITIONS[currentVertexId];
        return switch (side) {
            case NORTH -> new Vector3f(faceSize2, faceSize1, 1).mul(offset.y, offset.x, offset.z);
            case TOP -> new Vector3f(faceSize1, 1, faceSize2).mul(offset.x, offset.z, offset.y);
            case WEST -> new Vector3f(1, faceSize1, faceSize2).mul(offset.z, offset.y, offset.x);
            case SOUTH -> new Vector3f(faceSize2, faceSize1, 0).mul(offset.x, offset.y, offset.z);
            case BOTTOM -> new Vector3f(faceSize1, 0, faceSize2).mul(offset.y, offset.z, offset.x);
            case EAST -> new Vector3f(0, faceSize1, faceSize2).mul(offset.z, offset.x, offset.y);
            default -> new Vector3f(0);
        };
    }

    private static void generateModelBox(ArrayList<Float> vertices, ArrayList<Float> textureCoordinates, Vector3i size, Vector3f center,
                                         boolean padSize, Vector2f textureCoordinate) {
        float dx = size.x / 64F, dy = size.y / 64F, dz = size.z / 64F;
        float u = textureCoordinate.x, v = textureCoordinate.y;
        float padding = padSize ? 0.25F : 0;
        float px = size.x + padding - center.x, nx = -center.x - padding;
        float py = size.y + padding - center.y, ny = -center.y - padding;
        float pz = size.z + padding - center.z, nz = -center.z - padding;

        vertices.addAll(List.of(
                // north
                nx, py, pz,
                nx, ny, pz,
                px, py, pz,
                px, ny, pz,

                // top
                nx, py, nz,
                nx, py, pz,
                px, py, nz,
                px, py, pz,

                // west
                px, ny, nz,
                px, py, nz,
                px, ny, pz,
                px, py, pz,

                // south
                nx, ny, nz,
                nx, py, nz,
                px, ny, nz,
                px, py, nz,

                // bottom
                px, ny, pz,
                nx, ny, pz,
                px, ny, nz,
                nx, ny, nz,

                // east
                nx, py, nz,
                nx, ny, nz,
                nx, py, pz,
                nx, ny, pz
        ));
        textureCoordinates.addAll(List.of(
                // north
                u + 2 * dz + 1 * dx, v + dz,
                u + 2 * dz + 1 * dx, v + dy + dz,
                u + 2 * dz + 2 * dx, v + dz,
                u + 2 * dz + 2 * dx, v + dy + dz,

                // top
                u + dx + dz, v + dz,
                u + dx + dz, v,
                u + dz, v + dz,
                u + dz, v,

                // west
                u + dz, v + dy + dz,
                u + dz, v + dz,
                u, v + dy + dz,
                u, v + dz,

                // south
                u + dx + dz, v + dz + dy,
                u + dx + dz, v + dz,
                u + dz, v + dz + dy,
                u + dz, v + dz,

                // bottom
                u + dz + 2 * dx, v + dz,
                u + dz + dx, v + dz,
                u + dz + 2 * dx, v,
                u + dz + dx, v,

                // east
                u + dx + 1 * dz, v + dz,
                u + dx + 1 * dz, v + dy + dz,
                u + dx + 2 * dz, v + dz,
                u + dx + 2 * dz, v + dy + dz
        ));
    }

    private static float[] toFloatArray(ArrayList<Float> list) {
        float[] array = new float[list.size()];
        for (int index = 0; index < array.length; index++) array[index] = list.get(index);
        return array;
    }

    private static int[] toIntArray(ArrayList<Integer> list) {
        int[] array = new int[list.size()];
        for (int index = 0; index < array.length; index++) array[index] = list.get(index);
        return array;
    }

    private static final int VERTICES_PER_FACE = 4;
    private static final int VERTICES_PER_BOX = VERTICES_PER_FACE * 6;
    private static final Vector3f[] FACE_POSITIONS = new Vector3f[]{
            new Vector3f(0, 0, 1), new Vector3f(0, 1, 1), new Vector3f(1, 0, 1), new Vector3f(1, 1, 1)
    };
}
