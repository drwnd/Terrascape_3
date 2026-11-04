package game.player.interaction;

import core.assets.AssetManager;
import core.utils.Saver;

import game.assets.StructureIdentifier;
import game.items.Item;
import game.items.ItemType;
import game.server.generation.Structure;

public final class StructureSelector extends Item {

    public StructureSelector() {
        super(ItemType.STRUCTURE_SELECTOR);
    }

    @Override
    public PlacingState getPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat) {
        return isLocked ? PlacingState.STRUCTURE_SELECT_LOCKED : PlacingState.STRUCTURE_SELECT;
    }

    @Override
    public Structure getStructure() {
        return AssetManager.get(new StructureIdentifier("BluePrint"));
    }

    @Override
    public void saveSelf(Saver<?> saver) {

    }
}
