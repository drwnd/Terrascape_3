package game.entities.model;

import core.utils.FloatArrayList;
import core.utils.IntArrayList;

import game.player.rendering.MeshGenerator;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface ModelPart {
    Vector3f position();

    Integer parentIndex();

    boolean ignoreParentRotation();

    void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity);

    void addMesh(FloatArrayList vertices, FloatArrayList textureCoordinates, IntArrayList intData, MeshGenerator meshGenerator, int index);
}
