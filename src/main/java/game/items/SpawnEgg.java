package game.items;

import core.assets.AssetManager;
import core.utils.Saver;

import game.assets.EntityType;
import game.player.interaction.*;
import game.server.Game;
import game.server.generation.Structure;
import game.utils.Position;

import org.joml.Vector3f;

public final class SpawnEgg extends Item {

    public SpawnEgg(EntityType entityType) {
        super(ItemType.SPAWN_EGG);
        this.entityType = entityType;
    }

    public static SpawnEgg load(Saver<?> saver) {
        EntityType entityType = saver.loadEnum(EntityType.class);
        return new SpawnEgg(entityType);
    }

    @Override
    public void saveSelf(Saver<?> saver) {
        saver.saveEnum(entityType);
    }

    @Override
    public void use(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        VoxelTarget voxelTarget = VoxelTarget.getPlayerTarget();
        EntityTarget entityTarget = EntityTarget.getPlayerTarget();

        if (!interactionHandler.isTargetingVoxel(voxelTarget, entityTarget)) return;
        Game.getServer().spawnEntity(entityType.getEntity(new Position(voxelTarget.offsetPosition(), new Vector3f())));
        info.setUsed(Game.getServer().getCurrentGameTick());
    }

    @Override
    public PlacingState getPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat) {
        return PlacingState.NONE;
    }

    @Override
    public Structure getStructure() {
        return AssetManager.get(entityType.getStructureIdentifier());
    }

    @Override
    public String get() {
        String translation = super.get();
        return translation.formatted(entityType.get());
    }

    @Override
    public boolean rotateStructureDisplay() {
        return true;
    }

    private final EntityType entityType;
}
