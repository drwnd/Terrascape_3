package game.items;

import core.utils.MainThread;
import core.utils.Saver;

import game.player.interaction.InteractionHandler;
import game.player.interaction.PlaceDestroyInfo;
import game.player.interaction.PlacingState;
import game.player.interaction.VoxelTarget;
import game.server.Game;
import game.server.generation.Structure;

public abstract class Item implements Saver.Savable {

    public Item(ItemType itemType) {
        this.itemType = itemType;
    }

    @Override
    public final void save(Saver<?> saver) {
        saver.saveEnum(itemType);
        saveSelf(saver);
    }

    public static Item loadGeneric(Saver<?> saver) {
        ItemType type = saver.loadEnum(ItemType.class);
        return type.getItem(saver);
    }

    public void use(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        info.setUsed(Game.getServer().getCurrentGameTick());
    }

    public void attack(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        info.setUsed(Game.getServer().getCurrentGameTick());
    }

    public abstract void saveSelf(Saver<?> saver);

    public abstract PlacingState getPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat);

    @MainThread
    public abstract Structure getStructure();

    private final ItemType itemType;
}
