package game.player.interaction;

import core.rendering_api.Input;
import core.utils.Vector3l;
import core.utils.MainThread;

import game.player.interaction.placeable_shapes.CapsulePlaceable;
import game.player.interaction.placeable_shapes.CubePlaceable;
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

        if (button == KeySettings.DESTROY.keybind()) updateInfo(action, destroyInfo);
        if (button == KeySettings.USE.keybind()) updateInfo(action, useInfo);
    }

    @MainThread
    public static void handleInactiveInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(-1);
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(-1);
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
        if (!Input.isKeyPressed(KeySettings.DESTROY)) updateInfo(GLFW_RELEASE, destroyInfo);
        if (!Input.isKeyPressed(KeySettings.USE)) updateInfo(GLFW_RELEASE, useInfo);

        handleDestroy();
        handleUse();
    }

    public VoxelTarget getStartTarget() {
        return startTarget;
    }

    public VoxelTarget getLockedTarget() {
        return lockedTarget;
    }

    public PlacingState getState(VoxelTarget currentTarget) {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        boolean isLocked = lockedTarget != null;
        boolean isRepeat = startTarget != null;

        if (breakHeldOnly && !(placeable instanceof ShapePlaceable)) return PlacingState.NONE;
        if (placeable instanceof ChunkRebuildPlaceable) return PlacingState.NONE;
        if (placeable instanceof StructureSelector) return isLocked ? PlacingState.STRUCTURE_SELECT_LOCKED : PlacingState.STRUCTURE_SELECT;

        if (placeable instanceof CapsulePlaceable) {
            if (isLocked) return PlacingState.CAPSULE_LOCKED;
            if (currentTarget == null) return PlacingState.NONE;
            return PlacingState.CAPSULE;
        }

        if (placeable instanceof StructurePlaceable) {
            if (currentTarget == null && !isLocked) return PlacingState.NONE;
            return isLocked ? PlacingState.STRUCTURE_PLACE_LOCKED : PlacingState.STRUCTURE_PLACE;
        }

        if (isRepeat) {
            if (isLocked) return PlacingState.REPEAT_LOCKED;
            if (currentTarget == null) return PlacingState.NONE;
            return PlacingState.REPEAT;
        }
        if (isLocked) return PlacingState.SHAPE_LOCKED;
        if (currentTarget == null) return PlacingState.NONE;
        return PlacingState.SHAPE;
    }

    @ServerThread
    private void handleUse() {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        handleUseDestroy(useInfo, placeable, placeable != null && placeable.offsetOnPlace(), false);
    }

    @ServerThread
    private void handleDestroy() {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        if (placeable != null && !placeable.allowBreak()) placeable = null;
        else if (!(placeable instanceof ShapePlaceable shapePlaceable)) placeable = new CubePlaceable(AIR).setBitMapToFull();
        else placeable = shapePlaceable.copyWithMaterial(AIR);
        handleUseDestroy(destroyInfo, placeable, placeable != null && placeable.offsetOnBreak(), true);
    }

    @ServerThread
    private void handleUseDestroy(PlaceDestroyInfo info, Placeable placeable, boolean offsetPosition, boolean isDestroy) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (!info.forceAction && (!info.buttonIsHeld || currentGameTick - info.lastAction < IntSettings.BREAK_PLACE_INTERVALL.value())) return;
        info.forceAction = false;

        VoxelTarget voxelTarget = VoxelTarget.getPlayerTarget();
        EntityTarget entityTarget = EntityTarget.getPlayerTarget();

        if (entityTarget != null && (voxelTarget == null || entityTarget.distanceSquaredToPlayer() < voxelTarget.distanceSquaredToPlayer())) {
            Vector3f direction = Game.getPlayer().getCamera().getDirection();
            entityTarget.entity().hit(direction, isDestroy ? 1 : 0);
            info.lastAction = currentGameTick;
            info.forceAction = false;
            return;
        }
        if (!placeableCanModifyVoxels(placeable, isDestroy)) return;

        PlacingState state = getState(voxelTarget);
        VoxelTarget target = state.isLocked() ? lockedTarget : voxelTarget;

        if (target == null) {
            info.lastAction = currentGameTick;
            info.forceAction = false;
            return;
        }

        int targetedSide = target.side();
        Vector3l position = offsetPosition ? target.offsetPosition() : target.position();

        if (placeable instanceof CapsulePlaceable capsulePlaceable) {
            if (startTarget != null) target = startTarget;
            Vector3l startPosition = offsetPosition ? target.offsetPosition() : target.position();
            capsulePlaceable.setStartEndPositions(startPosition, position);
            targetedSide = target.side();

        } else if (placeable instanceof ShapePlaceable shapePlaceable) {
            if (startTarget != null) target = startTarget;
            Vector3l startPosition = offsetPosition ? target.offsetPosition() : target.position();
            placeable = new RepeatPlaceable(shapePlaceable, startPosition, position);
            targetedSide = target.side();
        }

        if (!Game.getServer().requestBreakPlaceInteraction(position, placeable, targetedSide)) return;
        info.lastAction = currentGameTick;
        startTarget = lockedTarget = null;
    }

    private boolean placeableCanModifyVoxels(Placeable placeable, boolean isDestroy) {
        if (placeable == null) return false;
        if (isDestroy)
            return placeable.allowBreak() && (OptionSettings.PLACE_MODE.value() != PlaceMode.BREAK_HELD_ONLY || placeable instanceof ShapePlaceable);
        else
            return placeable.allowPlace() && (OptionSettings.PLACE_MODE.value() != PlaceMode.BREAK_HELD_ONLY || !(placeable instanceof ShapePlaceable));
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
        if (Game.getPlayer().getHeldPlaceable() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
    }

    @MainThread
    private static void changeBreakPlaceAlign(int addend) {
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.clamp(IntSettings.BREAK_PLACE_ALIGN.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.max(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldPlaceable() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
    }

    private static void updateInfo(int action, PlaceDestroyInfo info) {
        info.buttonIsHeld = action == GLFW_PRESS;
        if (info.buttonIsHeld) info.forceAction = true;
    }


    private final PlaceDestroyInfo useInfo = new PlaceDestroyInfo();
    private final PlaceDestroyInfo destroyInfo = new PlaceDestroyInfo();
    private VoxelTarget startTarget = null, lockedTarget = null;

    private static class PlaceDestroyInfo {
        public long lastAction = 0;
        public boolean forceAction = false;
        public boolean buttonIsHeld = false;
    }
}
