package game.player.interaction;

import core.rendering_api.Input;
import core.utils.Vector3l;

import game.player.interaction.placeable_shapes.CubePlaceable;
import game.server.Game;
import game.settings.IntSettings;
import game.settings.KeySettings;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class InteractionHandler {

    public void handleInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(-1);
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(-1);

        if (action == GLFW_PRESS && button == KeySettings.LOCK_PLACE_POSITION.keybind()) handleLockPlacePosition();
        if (action == GLFW_PRESS && button == KeySettings.SET_PLACE_START_POSITION.keybind()) handleSetPlaceStartPosition();
        if (action == GLFW_RELEASE && button == KeySettings.SET_PLACE_START_POSITION.keybind()) handleReleasePlaceStartPosition();

        if (button == KeySettings.DESTROY.keybind()) updateInfo(action, destroyInfo);
        if (button == KeySettings.USE.keybind()) updateInfo(action, useInfo);
    }

    public void handleScroll(double yScroll) {
        int primaryDirection = Game.getPlayer().getCamera().getPrimaryDirection();
        Vector3i movement = new Vector3i(
                (primaryDirection == WEST ? 1 : 0) + (primaryDirection == EAST ? -1 : 0),
                (primaryDirection == TOP ? 1 : 0) + (primaryDirection == BOTTOM ? -1 : 0),
                (primaryDirection == NORTH ? 1 : 0) + (primaryDirection == SOUTH ? -1 : 0)
        ).mul(1 << IntSettings.BREAK_PLACE_ALIGN.value()).mul(yScroll > 0 ? 1 : -1);

        if (startTarget != null && Input.isKeyPressed(KeySettings.SNEAK)) startTarget.shiftPosition(movement);
        else if (lockedTarget != null && startTarget == null && !Input.isKeyPressed(KeySettings.SNEAK)) startTarget = new Target(lockedTarget);
        if (lockedTarget != null) lockedTarget.shiftPosition(movement);
    }

    public void updateGameTick() {
        if (!Input.isKeyPressed(KeySettings.DESTROY)) updateInfo(GLFW_RELEASE, destroyInfo);
        if (!Input.isKeyPressed(KeySettings.USE)) updateInfo(GLFW_RELEASE, useInfo);

        handleDestroy();
        handleUse();
    }

    public Target getStartTarget() {
        return startTarget;
    }

    public Target getLockedTarget() {
        return lockedTarget;
    }

    public PlacingState getState(Target currentTarget) {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        boolean isBreak = Input.isKeyPressed(KeySettings.SPRINT) || placeable == null;
        boolean isLocked = lockedTarget != null;
        boolean isRepeat = startTarget != null;

        if (placeable instanceof ChunkRebuildPlaceable) return PlacingState.NONE;
        if (placeable instanceof StructurePlaceable && !isBreak) {
            if (currentTarget == null && !isLocked) return PlacingState.NONE;
            return isLocked ? PlacingState.STRUCTURE_PLACE_LOCKED : PlacingState.STRUCTURE_PLACE;
        }

        if (isRepeat) {
            if (isLocked) return isBreak ? PlacingState.REPEAT_BREAK_LOCKED : PlacingState.REPEAT_PLACE_LOCKED;
            if (currentTarget == null) return PlacingState.NONE;
            return isBreak ? PlacingState.REPEAT_BREAK : PlacingState.REPEAT_PLACE;
        }
        if (isLocked) return isBreak ? PlacingState.SHAPE_BREAK_LOCKED : PlacingState.SHAPE_PLACE_LOCKED;
        if (currentTarget == null) return PlacingState.NONE;
        return isBreak ? PlacingState.SHAPE_BREAK : PlacingState.SHAPE_PLACE;
    }


    private void handleUse() {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        if (placeable == null) {
            useInfo.lastAction = Game.getServer().getCurrentGameTick();
            useInfo.forceAction = false;
            return;
        }
        handleUseDestroy(useInfo, placeable, true);
    }

    private void handleDestroy() {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        if (!(placeable instanceof ShapePlaceable shapePlaceable)) placeable = new CubePlaceable(AIR).setBitMapToFull();
        else placeable = shapePlaceable.copyWithMaterial(AIR);
        handleUseDestroy(destroyInfo, placeable, false);
    }

    private void handleUseDestroy(PlaceDestroyInfo info, Placeable placeable, boolean offsetPosition) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (!info.forceAction && (!info.buttonIsHeld || currentGameTick - info.lastAction < IntSettings.BREAK_PLACE_INTERVALL.value())) return;
        info.forceAction = false;

        Target currentTarget = Target.getPlayerTarget();
        PlacingState state = getState(currentTarget);
        Target target = state.isLocked() ? lockedTarget : currentTarget;

        if (target == null) {
            info.lastAction = currentGameTick;
            info.forceAction = false;
            return;
        }

        int targetedSide = target.side();
        Vector3l position = offsetPosition ? target.offsetPosition() : target.position();
        if (startTarget != null && placeable instanceof ShapePlaceable shapePlaceable) {
            Vector3l startPosition = offsetPosition ? startTarget.offsetPosition() : startTarget.position();
            placeable = new RepeatPlaceable(shapePlaceable, startPosition, position);
            targetedSide = startTarget.side();
            startTarget = null;
        }

        if (!Game.getServer().requestBreakPlaceInteraction(position, placeable, targetedSide)) return;
        info.lastAction = currentGameTick;
        startTarget = lockedTarget = null;
    }

    private void handleLockPlacePosition() {
        Target currentTarget = Target.getPlayerTarget();
        PlacingState state = getState(currentTarget);
        if (state.isLocked()) lockedTarget = startTarget = null;
        else lockedTarget = currentTarget;
    }

    private void handleSetPlaceStartPosition() {
        Target currentTarget = Target.getPlayerTarget();
        PlacingState state = getState(currentTarget);
        if (state == PlacingState.NONE || state.isLocked()) return;
        startTarget = currentTarget;
    }

    private void handleReleasePlaceStartPosition() {
        if (getState(Target.getPlayerTarget()).isLocked()) return;
        startTarget = null;
    }

    private static void changeBreakPlaceSize(int addend) {
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.clamp(IntSettings.BREAK_PLACE_SIZE.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.min(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldPlaceable() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap();
    }

    private static void changeBreakPlaceAlign(int addend) {
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.clamp(IntSettings.BREAK_PLACE_ALIGN.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.max(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldPlaceable() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap();
    }

    private static void updateInfo(int action, PlaceDestroyInfo info) {
        info.buttonIsHeld = action == GLFW_PRESS;
        if (info.buttonIsHeld) info.forceAction = true;
    }


    private final PlaceDestroyInfo useInfo = new PlaceDestroyInfo();
    private final PlaceDestroyInfo destroyInfo = new PlaceDestroyInfo();
    private Target startTarget = null, lockedTarget = null;

    private static class PlaceDestroyInfo {
        public long lastAction = 0;
        public boolean forceAction = false;
        public boolean buttonIsHeld = false;
    }
}
