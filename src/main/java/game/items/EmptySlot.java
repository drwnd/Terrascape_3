package game.items;

import core.utils.Saver;

import game.player.interaction.*;
import game.player.interaction.placeable_shapes.CubePlaceable;
import game.server.generation.Structure;

import static game.player.interaction.Placeable.getDefaultPlacingState;
import static game.utils.Constants.*;

public final class EmptySlot extends Item {

    public static final EmptySlot EMPTY_SLOT = new EmptySlot();
    
    private EmptySlot() {
        super(ItemType.NONE);
    }

    @Override
    public PlacingState getPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat) {
        return getDefaultPlacingState(currentTarget, isLocked, isRepeat);
    }

    @Override
    public Structure getStructure() {
        return null;
    }

    @Override
    public void attack(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        ShapePlaceable placeable = new CubePlaceable(AIR).setBitMapToFull();
        interactionHandler.attemptVoxelAndEntityInteraction(info, placeable, placeable.offsetOnBreak(), true);
    }

    @Override
    public void use(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        interactionHandler.attemptEntityInteraction(info, this, false);
    }

    @Override
    public void saveSelf(Saver<?> saver) {

    }
}
