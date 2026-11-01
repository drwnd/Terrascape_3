package game.entities.model;

import core.utils.FloatArrayList;
import core.utils.IntArrayList;

import game.player.particles.ParticleCollector;
import game.player.rendering.MeshGenerator;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public interface ModelPart {
    Vector3f position();

    Integer parentIndex();

    boolean ignoreParentRotation();

    Vector3i size();

    void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity);

    void spawnParticles(IntArrayList particles, Matrix4f transform, ParticleCollector collector, int inPartX, int inPartY, int inPartZ, int lengthX, int lengthY, int lengthZ);

    void addMesh(FloatArrayList vertices, FloatArrayList textureCoordinates, IntArrayList intData, MeshGenerator meshGenerator, int index);
}
