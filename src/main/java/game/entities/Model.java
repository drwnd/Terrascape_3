package game.entities;

import core.assets.*;
import core.utils.IntArrayList;
import core.utils.MainThread;

import game.assets.StructureIdentifier;
import game.player.particles.ParticleCollector;
import game.server.Game;
import game.server.generation.Structure;

import game.settings.IntSettings;
import org.joml.*;

public record Model(GuiElement guiElement, ModelPart[] parts) implements Asset {

    @MainThread
    public Model(ModelData data) {
        this(AssetLoader.loadGuiElement(data.toGuiElementData()), data.parts());
    }

    @Override
    public void delete() {
        guiElement.delete();
    }

    public interface ModelPart {
        Vector3f position();

        void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity);
    }

    record ModelBox(Vector3i size, Vector3f center, Vector3f position, Vector2f textureCoordinate, boolean hasOuterLayer, byte particleMaterial,
                    Vector2f outerTextureCoordinate) implements ModelPart {
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
    }

    record ModelStructure(Vector3f center, Vector3f position, String structureName) implements ModelPart {
        @Override
        public void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity) {
            transform.translate(-center.x, -center.y, -center.z);
            Structure structure = AssetManager.get(new StructureIdentifier(structureName));
            structure.materials().addDeathParticles(Game.getPlayer().getParticleCollector(), particles, transform, velocity);
        }
    }
}
