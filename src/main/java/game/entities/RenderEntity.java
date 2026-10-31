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
import org.joml.Vector4f;

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
        if (!(part instanceof StructurePart structurePart)) return;

        Structure structure = structurePart.structure();
        MaterialsData materialsData = part instanceof CustomModelStructure ? structure.materials() : new MaterialsData(structure.materials());

        int inChunkX = (int) (Math.random() * (structure.sizeX() + 1)) - WOUND_SIZE / 2;
        int inChunkY = (int) (Math.random() * (structure.sizeY() + 1)) - WOUND_SIZE / 2;
        int inChunkZ = (int) (Math.random() * (structure.sizeZ() + 1)) - WOUND_SIZE / 2;
        int lengthX = Math.min(inChunkX >= 0 ? structure.sizeX() - inChunkX : WOUND_SIZE + inChunkX, WOUND_SIZE);
        int lengthY = Math.min(inChunkY >= 0 ? structure.sizeY() - inChunkY : WOUND_SIZE + inChunkY, WOUND_SIZE);
        int lengthZ = Math.min(inChunkZ >= 0 ? structure.sizeZ() - inChunkZ : WOUND_SIZE + inChunkZ, WOUND_SIZE);

        spawnParticles(position, structurePart, structure, index, inChunkX, inChunkY, inChunkZ, lengthX, lengthY, lengthZ);
        breakVoxels(materialsData, inChunkX, inChunkY, inChunkZ, lengthX, lengthY, lengthZ);

        ModelPart[] newParts = isCustomModel ? parts : parts.clone();
        Structure newStructure = new Structure(structure.sizeX(), structure.sizeY(), structure.sizeZ(), materialsData);
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

    private void spawnParticles(Position position, StructurePart structurePart, Structure structure, int index, int inChunkX, int inChunkY, int inChunkZ, int lengthX, int lengthY, int lengthZ) {
        IntArrayList particles = new IntArrayList(64);
        ParticleCollector collector = Game.getPlayer().getParticleCollector();
        Vector3f center = structurePart.center(), velocity = new Vector3f();
        Matrix4f transform = new Matrix4f(parentedTransforms[index].translate(-center.x, -center.y, -center.z));
        Vector4f current = new Vector4f();

        int startX = Math.max(0, inChunkX), startY = Math.max(0, inChunkY), startZ = Math.max(0, inChunkZ);
        for (int x = startX; x < startX + lengthX; x++)
            for (int y = startY; y < startY + lengthY; y++)
                for (int z = startZ; z < startZ + lengthZ; z++) {

                    byte material = structure.getMaterial(x, y, z);
                    if (material == AIR || WOUND_STRUCTURE.getMaterial(x - inChunkX, y - inChunkY, z - inChunkZ) == AIR) continue;
                    current.set(x, y, z, 1).mul(transform);
                    collector.addDeathParticle(particles, velocity, (int) current.x, (int) current.y, (int) current.z, material);
                }
        collector.addParticles(position.longX, position.longY, position.longZ, particles, ParticleType.OPAQUE_BREAK);
    }

    private void breakVoxels(MaterialsData materials, int inChunkX, int inChunkY, int inChunkZ, int lengthX, int lengthY, int lengthZ) {
        byte[] uncompressedMaterials = new byte[1 << materials.getTotalSizeBits() * 3];
        materials.fillUncompressedMaterialsInto(uncompressedMaterials);

        int startX = Math.max(0, inChunkX), startY = Math.max(0, inChunkY), startZ = Math.max(0, inChunkZ);
        for (int x = startX; x < startX + lengthX; x++)
            for (int y = startY; y < startY + lengthY; y++)
                for (int z = startZ; z < startZ + lengthZ; z++) {

                    if (WOUND_STRUCTURE.getMaterial(x - inChunkX, y - inChunkY, z - inChunkZ) == AIR) continue;
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
    private static final Structure WOUND_STRUCTURE = new Structure(2, STONE, new long[]{0x172b4d8e71b2d4e8L});
    private static final int WOUND_SIZE = 4;
}
