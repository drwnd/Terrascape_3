package game.entities.model;

import core.utils.FloatArrayList;
import core.utils.IntArrayList;

import game.player.particles.ParticleCollector;
import game.player.rendering.Mesh;
import game.player.rendering.MeshGenerator;
import game.server.Game;
import game.server.generation.Structure;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import static game.entities.RenderEntity.*;
import static game.entities.model.ModelData.*;
import static game.utils.Constants.*;

public interface StructurePart extends ModelPart {

    Structure structure();

    Vector3f center();

    Vector3f position();

    @Override
    default Vector3i size() {
        Structure structure = structure();
        return new Vector3i(structure.sizeX(), structure.sizeY(), structure.sizeZ());
    }

    @Override
    default void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity) {
        Vector3f center = center();
        transform.translate(-center.x, -center.y, -center.z);
        structure().materials().addDeathParticles(Game.getPlayer().getParticleCollector(), particles, transform, velocity);
    }

    @Override
    default void spawnParticles(IntArrayList particles, Matrix4f transform, ParticleCollector collector, int inPartX, int inPartY, int inPartZ, int lengthX, int lengthY, int lengthZ) {
        Vector3f center = center(), velocity = new Vector3f();
        transform = new Matrix4f(transform).translate(-center.x, -center.y, -center.z);
        Vector4f current = new Vector4f();
        Structure structure = structure();

        int startX = Math.max(0, inPartX), startY = Math.max(0, inPartY), startZ = Math.max(0, inPartZ);
        for (int x = startX; x < startX + lengthX; x++)
            for (int y = startY; y < startY + lengthY; y++)
                for (int z = startZ; z < startZ + lengthZ; z++) {

                    byte material = structure.getMaterial(x, y, z);
                    if (material == AIR || WOUND_STRUCTURE.getMaterial(x - inPartX, y - inPartY, z - inPartZ) == AIR) continue;
                    current.set(x, y, z, 1).mul(transform);
                    collector.addDeathParticle(particles, velocity, (int) current.x, (int) current.y, (int) current.z, material);
                }
    }

    @Override
    default void addMesh(FloatArrayList vertices, FloatArrayList textureCoordinates, IntArrayList intData, MeshGenerator generator, int index) {
        Structure structure = structure();

        Mesh mesh = generator.generateMesh(structure);
        int[] opaqueVertices = mesh.opaqueVertices();
        for (int vertexIndex = 0; vertexIndex < opaqueVertices.length; vertexIndex += MeshGenerator.INTS_PER_VERTEX) {
            int x = opaqueVertices[vertexIndex + 0];
            int y = opaqueVertices[vertexIndex + 1];
            int z = opaqueVertices[vertexIndex + 2];
            int textureData = opaqueVertices[vertexIndex + 3];
            generateModelFace(vertices, textureCoordinates, center(), x, y, z, textureData);

            int side = textureData >> 8 & 7;
            int material = textureData & 0xFF;
            for (int count = 0; count < VERTICES_PER_FACE; count++) intData.add(side << 29 | material << 21 | index);
        }
    }

    private static void generateModelFace(FloatArrayList vertices, FloatArrayList textureCoordinates, Vector3f center, int x, int y, int z, int textureData) {
        int side = textureData >> 8 & 7;

        float faceSize1 = (textureData >> 17 & 63) + 1;
        float faceSize2 = (textureData >> 11 & 63) + 1;
        Vector3f pos0 = getFacePosition(side, 0, faceSize1, faceSize2).sub(center).add(x, y, z);
        Vector3f pos1 = getFacePosition(side, 1, faceSize1, faceSize2).sub(center).add(x, y, z);
        Vector3f pos2 = getFacePosition(side, 2, faceSize1, faceSize2).sub(center).add(x, y, z);
        Vector3f pos3 = getFacePosition(side, 3, faceSize1, faceSize2).sub(center).add(x, y, z);

        vertices.addAll(new float[]{
                pos0.x, pos0.y, pos0.z,
                pos1.x, pos1.y, pos1.z,
                pos2.x, pos2.y, pos2.z,
                pos3.x, pos3.y, pos3.z
        });

        if (side == NORTH || side == TOP || side == EAST) {
            float temp = faceSize1;
            faceSize1 = faceSize2;
            faceSize2 = temp;
        }

        textureCoordinates.addAll(new float[]{
                0.0F, 0.0F,
                faceSize1, 0.0F,
                0.0F, -faceSize2,
                faceSize1, -faceSize2
        });
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
}
