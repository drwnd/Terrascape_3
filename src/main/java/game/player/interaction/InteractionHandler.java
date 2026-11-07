package game.player.interaction;

import core.rendering_api.Input;
import core.utils.Vector3l;
import core.utils.MainThread;

import game.items.Item;
import game.player.interaction.placeable_shapes.CapsulePlaceable;
import game.server.Game;
import game.settings.IntSettings;
import game.settings.KeySettings;
import game.settings.OptionSettings;
import game.utils.ServerThread;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class InteractionHandler {

    @MainThread
    public void handleActiveInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.LOCK_PLACE_POSITION.keybind()) handleLockPlacePosition();
        if (action == GLFW_PRESS && button == KeySettings.SET_PLACE_START_POSITION.keybind()) handleSetPlaceStartPosition();
        if (action == GLFW_RELEASE && button == KeySettings.SET_PLACE_START_POSITION.keybind()) handleReleasePlaceStartPosition();

        if (button == KeySettings.DESTROY.keybind()) destroyInfo.update(action);
        if (button == KeySettings.USE.keybind()) useInfo.update(action);
    }

    @MainThread
    public static void handleInactiveInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(-1);
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(-1);
    }

    public static boolean voxelTargetIsCloser(EntityTarget entityTarget, VoxelTarget voxelTarget) {
        return entityTarget == null || voxelTarget != null && voxelTarget.distanceSquaredToPlayer() < entityTarget.distanceSquaredToPlayer();
    }

    public static boolean entityTargetIsCloser(VoxelTarget voxelTarget, EntityTarget entityTarget) {
        return voxelTarget == null || entityTarget != null && entityTarget.distanceSquaredToPlayer() < voxelTarget.distanceSquaredToPlayer();
    }

    @MainThread
    public void handleScroll(double yScroll) {
        int primaryDirection = Game.getPlayer().getCamera().getPrimaryDirection();
        Vector3i movement = new Vector3i(
                (primaryDirection == WEST ? 1 : 0) + (primaryDirection == EAST ? -1 : 0),
                (primaryDirection == TOP ? 1 : 0) + (primaryDirection == BOTTOM ? -1 : 0),
                (primaryDirection == NORTH ? 1 : 0) + (primaryDirection == SOUTH ? -1 : 0)
        ).mul(1 << IntSettings.BREAK_PLACE_ALIGN.value()).mul(yScroll > 0 ? 1 : -1);

        if (startTarget != null && Input.isKeyPressed(KeySettings.SNEAK)) startTarget.shiftPosition(movement);
        else if (lockedTarget != null && startTarget == null && !Input.isKeyPressed(KeySettings.SNEAK)) startTarget = new VoxelTarget(lockedTarget);
        if (lockedTarget != null) lockedTarget.shiftPosition(movement);
    }

    @ServerThread
    public void updateGameTick() {
        if (!Input.isKeyPressed(KeySettings.DESTROY)) destroyInfo.update(GLFW_RELEASE);
        if (!Input.isKeyPressed(KeySettings.USE)) useInfo.update(GLFW_RELEASE);

        if (canUse(useInfo)) Game.getPlayer().getHeldItem().use(this, useInfo);
        if (canUse(destroyInfo)) Game.getPlayer().getHeldItem().attack(this, destroyInfo);
    }

    public VoxelTarget getStartTarget() {
        return startTarget;
    }

    public VoxelTarget getLockedTarget() {
        return lockedTarget;
    }

    public PlacingState getState(VoxelTarget currentTarget) {
        Item item = Game.getPlayer().getHeldItem();
        boolean isLocked = lockedTarget != null;
        boolean isRepeat = startTarget != null;
        return item.getPlacingState(currentTarget, isLocked, isRepeat);
    }

    @ServerThread
    public void attemptEntityInteraction(PlaceDestroyInfo info, Item item, boolean isDestroy) {
        long currentGameTick = Game.getServer().getCurrentGameTick();

        VoxelTarget voxelTarget = VoxelTarget.getPlayerTarget();
        EntityTarget entityTarget = EntityTarget.getPlayerTarget();
        if (voxelTargetIsCloser(entityTarget, voxelTarget)) return;
        interactWithEntity(info, entityTarget, item, currentGameTick, isDestroy);
    }

    @ServerThread
    public void attemptVoxelInteraction(PlaceDestroyInfo info, Placeable placeable, boolean offsetPosition, boolean isDestroy) {
        long currentGameTick = Game.getServer().getCurrentGameTick();

        VoxelTarget voxelTarget = VoxelTarget.getPlayerTarget();
        EntityTarget entityTarget = EntityTarget.getPlayerTarget();
        if (entityTargetIsCloser(voxelTarget, entityTarget)) return;
        interactWithVoxel(info, placeable, voxelTarget, currentGameTick, offsetPosition, isDestroy);
    }

    @ServerThread
    public void attemptVoxelAndEntityInteraction(PlaceDestroyInfo info, Item item, boolean offsetPosition, boolean isDestroy) {
        long currentGameTick = Game.getServer().getCurrentGameTick();

        VoxelTarget voxelTarget = VoxelTarget.getPlayerTarget();
        EntityTarget entityTarget = EntityTarget.getPlayerTarget();
        if (entityTargetIsCloser(voxelTarget, entityTarget))
            interactWithEntity(info, entityTarget, item, currentGameTick, isDestroy);
        if (voxelTargetIsCloser(entityTarget, voxelTarget))
            interactWithVoxel(info, item, voxelTarget, currentGameTick, offsetPosition, isDestroy);
    }

    @ServerThread
    private void interactWithVoxel(PlaceDestroyInfo info, Item item, VoxelTarget voxelTarget, long currentGameTick, boolean offsetPosition, boolean isDestroy) {
        if (!itemCanModifyVoxels(item, isDestroy)) return;
        PlacingState state = getState(voxelTarget);
        VoxelTarget target = state.isLocked() ? lockedTarget : voxelTarget;

        if (target == null) {
            info.setUsed(currentGameTick);
            return;
        }

        int targetedSide = target.side();
        Vector3l position = offsetPosition ? target.offsetPosition() : target.position();
        Placeable placeable = (Placeable) item;

        if (item instanceof CapsulePlaceable capsulePlaceable) {
            if (startTarget != null) target = startTarget;
            Vector3l startPosition = offsetPosition ? target.offsetPosition() : target.position();
            capsulePlaceable.setStartEndPositions(startPosition, position);
            targetedSide = target.side();

        } else if (item instanceof ShapePlaceable shapePlaceable) {
            if (startTarget != null) target = startTarget;
            Vector3l startPosition = offsetPosition ? target.offsetPosition() : target.position();
            placeable = new RepeatPlaceable(shapePlaceable, startPosition, position);
            targetedSide = target.side();
        }

        if (!Game.getServer().requestBreakPlaceInteraction(position, placeable, targetedSide)) return;
        info.setUsed(currentGameTick);
        startTarget = lockedTarget = null;
    }

    @ServerThread
    private static void interactWithEntity(PlaceDestroyInfo info, EntityTarget entityTarget, Item item, long currentGameTick, boolean isDestroy) {
        Vector3f direction = Game.getPlayer().getCamera().getDirection();
        entityTarget.entity().hit(direction, isDestroy ? 1 : 0);
        info.setUsed(currentGameTick);
    }

    private boolean itemCanModifyVoxels(Item item, boolean isDestroy) {
        if (!(item instanceof Placeable placeable)) return false;
        if (isDestroy)
            return placeable.allowBreak() && (OptionSettings.PLACE_MODE.value() != PlaceMode.BREAK_HELD_ONLY || placeable instanceof ShapePlaceable);
        else
            return placeable.allowPlace() && (OptionSettings.PLACE_MODE.value() != PlaceMode.BREAK_HELD_ONLY || !(placeable instanceof ShapePlaceable));
    }

    private boolean canUse(PlaceDestroyInfo info) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (!info.forceAction && (!info.buttonIsHeld || currentGameTick - info.lastAction < IntSettings.BREAK_PLACE_INTERVALL.value())) return false;
        info.forceAction = false;
        return true;
    }

    @MainThread
    private void handleLockPlacePosition() {
        VoxelTarget currentTarget = VoxelTarget.getPlayerTarget();
        PlacingState state = getState(currentTarget);
        if (state.isLocked()) lockedTarget = startTarget = null;
        else if (currentTarget != null) {
            lockedTarget = currentTarget;
            if (startTarget == null) startTarget = new VoxelTarget(currentTarget);
        }
    }

    @MainThread
    private void handleSetPlaceStartPosition() {
        VoxelTarget currentTarget = VoxelTarget.getPlayerTarget();
        PlacingState state = getState(currentTarget);
        if (state == PlacingState.NONE || state.isLocked()) return;
        startTarget = currentTarget;
    }

    @MainThread
    private void handleReleasePlaceStartPosition() {
        if (getState(VoxelTarget.getPlayerTarget()).isLocked()) return;
        startTarget = null;
    }

    @MainThread
    private static void changeBreakPlaceSize(int addend) {
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.clamp(IntSettings.BREAK_PLACE_SIZE.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.min(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldItem() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
    }

    @MainThread
    private static void changeBreakPlaceAlign(int addend) {
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.clamp(IntSettings.BREAK_PLACE_ALIGN.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.max(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldItem() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
    }


    private final PlaceDestroyInfo useInfo = new PlaceDestroyInfo();
    private final PlaceDestroyInfo destroyInfo = new PlaceDestroyInfo();
    private VoxelTarget startTarget = null, lockedTarget = null;

}
