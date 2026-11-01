package game.entities;

import core.assets.AssetLoader;
import core.assets.AssetManager;
import core.assets.GuiElement;
import core.assets.Texture2D;
import core.assets.identifiers.AssetIdentifier;
import core.utils.IntArrayList;
import core.utils.MainThread;

import game.entities.model.*;
import game.player.particles.ParticleCollector;
import game.player.particles.ParticleType;
import game.server.Game;
import game.server.generation.Structure;
import game.server.materials_data.MaterialsData;
import game.utils.Position;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public class RenderEntity {

    public double animationTimer = 0;

    public RenderEntity(AssetIdentifier<Model> modelIdentifier, AssetIdentifier<Texture2D> textureIdentifier, Animator animator) {
        this.modelIdentifier = modelIdentifier;
        this.animator = animator;
        this.textureIdentifier = textureIdentifier;
    }

    public RenderEntity(AssetIdentifier<Model> modelIdentifier, Animator animator) {
        this.modelIdentifier = modelIdentifier;
        this.animator = animator;
        this.textureIdentifier = NULL_TEXTURE_IDENTIFIER;
    }

    public RenderEntity(AssetIdentifier<Model> modelIdentifier) {
        this.modelIdentifier = modelIdentifier;
        animator = Animator.DEFAULT_ANIMATOR;
        this.textureIdentifier = NULL_TEXTURE_IDENTIFIER;
    }

    @MainThread
    public void updateFrame(float frameTime, float gameTickFraction, Vector3f rotation) {
        synchronizeTransforms();
        animator.applyAnimation(this, rotation, frameTime, gameTickFraction);
    }

    public void synchronizeTransforms() {
        model = AssetManager.get(modelIdentifier);
        if (transforms.length != model.parts().length) {
            transforms = new Matrix4f[model.parts().length];
            parentedTransforms = new Matrix4f[model.parts().length];
            for (int index = 0; index < transforms.length; index++) transforms[index] = new Matrix4f().zero();
            System.arraycopy(transforms, 0, parentedTransforms, 0, model.parts().length);
        }
    }

    public void setAnimator(Animator animator) {
        this.animator = animator;
    }

    @MainThread
    public void hit(Position position) {
        ModelPart[] parts = model.parts();
        int index = (int) (Math.random() * parts.length);
        ModelPart part = parts[index];
        Vector3i size = part.size();

        int inPartX = (int) (Math.random() * (size.x + 1)) - WOUND_SIZE / 2;
        int inPartY = (int) (Math.random() * (size.y + 1)) - WOUND_SIZE / 2;
        int inPartZ = (int) (Math.random() * (size.z + 1)) - WOUND_SIZE / 2;
        int lengthX = Math.min(inPartX >= 0 ? size.x - inPartX : WOUND_SIZE + inPartX, WOUND_SIZE);
        int lengthY = Math.min(inPartY >= 0 ? size.y - inPartY : WOUND_SIZE + inPartY, WOUND_SIZE);
        int lengthZ = Math.min(inPartZ >= 0 ? size.z - inPartZ : WOUND_SIZE + inPartZ, WOUND_SIZE);

        IntArrayList particles = new IntArrayList(64);
        ParticleCollector collector = Game.getPlayer().getParticleCollector();
        part.spawnParticles(particles, parentedTransforms[index], collector, inPartX, inPartY, inPartZ, lengthX, lengthY, lengthZ);
        collector.addParticles(position.longX, position.longY, position.longZ, particles, ParticleType.OPAQUE_BREAK);

        if (!(part instanceof StructurePart structurePart)) return;
        Structure structure = structurePart.structure();
        MaterialsData materialsData = part instanceof CustomModelStructure ? structure.materials() : new MaterialsData(structure.materials());
        breakVoxels(materialsData, inPartX, inPartY, inPartZ, lengthX, lengthY, lengthZ);

        ModelPart[] newParts = isCustomModel ? parts : parts.clone();
        Structure newStructure = new Structure(size.x, size.y, size.z, materialsData);
        newParts[index] = new CustomModelStructure(structurePart.center(), structurePart.position(), newStructure, structurePart.parentIndex(), structurePart.ignoreParentRotation());

        AssetIdentifier<Model> oldIdentifier = modelIdentifier;
        modelIdentifier = () -> loadModel(newParts);
        if (isCustomModel) AssetManager.deleteOnFrameUpdate(oldIdentifier);
        isCustomModel = true;
    }

    @MainThread
    private static Model loadModel(ModelPart[] parts) {
        return new Model(AssetLoader.loadGuiElement(ModelData.toGuiElementData(parts)), parts);
    }

    private void breakVoxels(MaterialsData materials, int inPartX, int inPartY, int inPartZ, int lengthX, int lengthY, int lengthZ) {
        byte[] uncompressedMaterials = new byte[1 << materials.getTotalSizeBits() * 3];
        materials.fillUncompressedMaterialsInto(uncompressedMaterials);

        int startX = Math.max(0, inPartX), startY = Math.max(0, inPartY), startZ = Math.max(0, inPartZ);
        for (int x = startX; x < startX + lengthX; x++)
            for (int y = startY; y < startY + lengthY; y++)
                for (int z = startZ; z < startZ + lengthZ; z++) {

                    if (WOUND_STRUCTURE.getMaterial(x - inPartX, y - inPartY, z - inPartZ) == AIR) continue;
                    int uncompressedIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    uncompressedMaterials[uncompressedIndex] = AIR;
                }
        materials.compressIntoData(uncompressedMaterials);
    }

    public Matrix4f[] transforms() {
        return transforms;
    }

    public Matrix4f[] parentedTransforms() {
        return parentedTransforms;
    }

    public ModelPart[] parts() {
        return model.parts();
    }

    public GuiElement guiElement() {
        return model.guiElement();
    }

    public AssetIdentifier<Model> getModelIdentifier() {
        return modelIdentifier;
    }

    public boolean isCustomModel() {
        return isCustomModel;
    }

    public AssetIdentifier<Texture2D> getTextureIdentifier() {
        return textureIdentifier;
    }

    private final AssetIdentifier<Texture2D> textureIdentifier;
    private Animator animator;
    private Matrix4f[] transforms = new Matrix4f[0], parentedTransforms = new Matrix4f[0];
    private AssetIdentifier<Model> modelIdentifier;
    private Model model;
    private boolean isCustomModel = false;

    private static final AssetIdentifier<Texture2D> NULL_TEXTURE_IDENTIFIER = () -> new Texture2D(0);
    public static final Structure WOUND_STRUCTURE = new Structure(2, STONE, new long[]{0x172b4d8e71b2d4e8L});
    public static final int WOUND_SIZE = 4;
}
