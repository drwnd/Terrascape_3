package game.items;

import core.language.Translatable;
import core.utils.MainThread;
import core.utils.Saver;

import game.player.interaction.InteractionHandler;
import game.player.interaction.PlaceDestroyInfo;
import game.player.interaction.PlacingState;
import game.player.interaction.VoxelTarget;
import game.server.Game;
import game.server.generation.Structure;
import game.utils.ServerThread;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class Item implements Saver.Savable, Translatable {

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

    @ServerThread
    public void use(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        info.setUsed(Game.getServer().getCurrentGameTick());
    }

    @ServerThread
    public void attack(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        info.setUsed(Game.getServer().getCurrentGameTick());
    }

    @Override
    public String translationFileName() {
        return "items";
    }

    @Override
    public int ordinal() {
        return itemType.ordinal();
    }

    public boolean rotateStructureDisplay() {
        return false;
    }

    public abstract void saveSelf(Saver<?> saver);

    public abstract PlacingState getPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat);

    public Matrix4f getModelMatrixWhenHeld(Vector3f handPosition) {
        return new Matrix4f().translate(handPosition);
    }

    @MainThread
    public abstract Structure getStructure();

    @MainThread
    public Structure getHeldStructure() {
        return getStructure();
    }

    private final ItemType itemType;
}
