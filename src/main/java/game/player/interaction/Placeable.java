package game.player.interaction;

import core.utils.MathUtils;
import core.utils.Vector3l;

import game.items.Item;
import game.items.ItemType;
import game.player.interaction.placeable_shapes.CubePlaceable;
import game.server.Chunk;
import game.settings.IntSettings;
import game.utils.ServerThread;

import java.util.ArrayList;

import static game.utils.Constants.*;

public abstract class Placeable extends Item {

    public Placeable(ItemType itemType) {
        super(itemType);
    }

    public static PlacingState getDefaultPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat) {
        if (isRepeat) {
            if (isLocked) return PlacingState.REPEAT_LOCKED;
            if (currentTarget == null) return PlacingState.NONE;
            return PlacingState.REPEAT;
        }
        if (isLocked) return PlacingState.SHAPE_LOCKED;
        if (currentTarget == null) return PlacingState.NONE;
        return PlacingState.SHAPE;
    }

    @Override
    public void use(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        interactionHandler.attemptVoxelAndEntityInteraction(info, this, offsetOnPlace(), false);
    }

    @Override
    public void attack(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        Placeable placeable;
        if (!allowBreak()) placeable = null;
        else if (!(this instanceof ShapePlaceable shapePlaceable)) placeable = new CubePlaceable(AIR).setBitMapToFull();
        else placeable = shapePlaceable.copyWithMaterial(AIR);
        interactionHandler.attemptVoxelAndEntityInteraction(info, placeable, offsetOnBreak(), true);
    }

    @ServerThread
    public abstract void place(Vector3l position, int lod);

    @ServerThread
    public abstract ArrayList<Chunk> getAffectedChunks();

    @ServerThread
    public abstract boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max);

    @ServerThread
    public abstract void offsetPosition(Vector3l position, int targetedSide);

    @ServerThread
    public abstract void spawnParticles(Vector3l position);

    @ServerThread
    public abstract void playSounds(Vector3l position);

    public void rotateForwards() {
    }

    public void rotateBackwards() {
    }

    public boolean allowBreak() {
        return true;
    }

    public boolean allowPlace() {
        return true;
    }

    public boolean offsetOnPlace() {
        return true;
    }

    public boolean offsetOnBreak() {
        return false;
    }

    public int getLengthX() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    public int getLengthY() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    public int getLengthZ() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    public int getPreferredSizePowOf2() {
        return MathUtils.nextLargestPowOf2(getPreferredSize());
    }

    public int getPreferredSize() {
        return Math.max(getLengthX(), Math.max(getLengthY(), getLengthZ()));
    }
}
