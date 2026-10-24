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

import java.util.Arrays;

public record Model(GuiElement guiElement, ModelPart[] parts) implements Asset {

    @MainThread
    public Model(ModelData data) {
        this(AssetLoader.loadGuiElement(data.toGuiElementData()), data.parts());
    }

    public Model(GuiElement guiElement, ModelPart[] parts) {
        this.guiElement = guiElement;
        this.parts = parts;
        if (!areParentIndicesValid(parts)) throw new IllegalStateException("The models parent-graph is not valid");
    }

    @Override
    public void delete() {
        guiElement.delete();
    }


    private static boolean areParentIndicesValid(ModelPart[] parts) {
        for (ModelPart part : parts) if (part == null) return false;
        boolean[] touched = new boolean[parts.length];

        for (int index = 0; index < parts.length; index++) {
            Arrays.fill(touched, false);
            ModelPart part = parts[index];

            int currentIndex = index;
            while (part.parentIndex() != null) {
                if (part.parentIndex() >= parts.length || touched[currentIndex]) return false;
                touched[currentIndex] = true;
                currentIndex = part.parentIndex();
                part = parts[currentIndex];
            }
        }
        return true;
    }


    public interface ModelPart {
        Vector3f position();

        Integer parentIndex();

        void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity);
    }

    record ModelBox(Vector3i size, Vector3f center, Vector3f position, Vector2f textureCoordinate, byte particleMaterial,
                    Vector2f outerTextureCoordinate, Integer parentIndex) implements ModelPart {
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

        public boolean hasOuterLayer() {
            return outerTextureCoordinate != null;
        }
    }

    record ModelStructure(Vector3f center, Vector3f position, String structureName, Integer parentIndex) implements ModelPart {
        @Override
        public void spawnParticles(IntArrayList particles, Matrix4f transform, Vector3f velocity) {
            transform.translate(-center.x, -center.y, -center.z);
            Structure structure = AssetManager.get(new StructureIdentifier(structureName));
            structure.materials().addDeathParticles(Game.getPlayer().getParticleCollector(), particles, transform, velocity);
        }
    }
}
