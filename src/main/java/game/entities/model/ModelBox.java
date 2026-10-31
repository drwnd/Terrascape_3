package game.entities.model;

import core.utils.FloatArrayList;
import core.utils.IntArrayList;
import game.player.particles.ParticleCollector;
import game.player.rendering.MeshGenerator;
import game.server.Game;
import game.settings.IntSettings;
import org.joml.*;

import static game.entities.model.ModelData.*;

public record ModelBox(Vector3i size, Vector3f center, Vector3f position, Vector2f textureCoordinate, byte particleMaterial,
                Vector2f outerTextureCoordinate, Integer parentIndex, boolean ignoreParentRotation) implements ModelPart {
    @Override
    public void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity) {
        transform.translate(-center.x, -center.y, -center.z);
        ParticleCollector collector = Game.getPlayer().getParticleCollector();
        Vector4f position = new Vector4f();
        int stepLength = IntSettings.DEATH_PARTICLE_STEP_LENGTH.value();
        for (int x = 0; x < size.x; x += stepLength)
            for (int y = 0; y < size.y; y += stepLength)
                for (int z = 0; z < size.z; z += stepLength) {
                    position.set(x, y, z, 1).mul(transform);
                    collector.addDeathParticle(particles, velocity, (int) position.x, (int) position.y, (int) position.z, particleMaterial);
                }
    }

    @Override
    public void addMesh(FloatArrayList vertices, FloatArrayList textureCoordinates, IntArrayList intData, MeshGenerator meshGenerator, int index) {
        int amount = hasOuterLayer() ? VERTICES_PER_BOX * 2 : VERTICES_PER_BOX * 1;

        generateModelBox(vertices, textureCoordinates, false, textureCoordinate());
        if (hasOuterLayer()) generateModelBox(vertices, textureCoordinates, true, outerTextureCoordinate());

        for (int count = 0; count < amount; count++) intData.add((count / VERTICES_PER_FACE % 6) << 29 | index);
    }

    public boolean hasOuterLayer() {
        return outerTextureCoordinate != null;
    }

    private void generateModelBox(FloatArrayList vertices, FloatArrayList textureCoordinates, boolean padSize, Vector2f textureCoordinate) {
        float dx = size.x / 64F, dy = size.y / 64F, dz = size.z / 64F;
        float u = textureCoordinate.x, v = textureCoordinate.y;
        float padding = padSize ? 0.25F : 0;
        float px = size.x + padding - center.x, nx = -center.x - padding;
        float py = size.y + padding - center.y, ny = -center.y - padding;
        float pz = size.z + padding - center.z, nz = -center.z - padding;

        vertices.addAll(new float[]{
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
        });
        textureCoordinates.addAll(new float[]{
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
        });
    }
}
