package game.items;

import core.assets.AssetManager;
import core.utils.Saver;
import game.assets.StructureIdentifier;
import game.player.interaction.InteractionHandler;
import game.player.interaction.PlaceDestroyInfo;
import game.player.interaction.PlacingState;
import game.player.interaction.VoxelTarget;
import game.server.generation.Structure;
import game.utils.ServerThread;

public final class Tool extends Item {

    public Tool(Saver<?> saver) {
        super(ItemType.TOOL);
        type = saver.loadEnum(ToolType.class);
        grade = saver.loadEnum(ToolGrade.class);
        structureIdentifier = createStructureIdentifier(grade, type);
    }

    public Tool(ToolType type, ToolGrade grade) {
        super(ItemType.TOOL);
        this.type = type;
        this.grade = grade;
        structureIdentifier = createStructureIdentifier(grade, type);
    }

    @Override
    @ServerThread
    public void use(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        interactionHandler.attemptEntityInteraction(info, this, false);
    }

    @Override
    @ServerThread
    public void attack(InteractionHandler interactionHandler, PlaceDestroyInfo info) {
        interactionHandler.attemptEntityInteraction(info, this, true);
    }

    @Override
    public void saveSelf(Saver<?> saver) {
        saver.saveEnum(type);
        saver.saveEnum(grade);
    }

    @Override
    public PlacingState getPlacingState(VoxelTarget currentTarget, boolean isLocked, boolean isRepeat) {
        return PlacingState.NONE;
    }

    @Override
    public Structure getStructure() {
        return AssetManager.get(structureIdentifier);
    }

    @Override
    public String get() {
        return "%s %s".formatted(grade.get(), type.get());
    }

    private static StructureIdentifier createStructureIdentifier(ToolGrade grade, ToolType type) {
        return new StructureIdentifier("%s %s".formatted(grade.name(), type.name()));
    }

    private final ToolType type;
    private final ToolGrade grade;
    private final StructureIdentifier structureIdentifier;
}
