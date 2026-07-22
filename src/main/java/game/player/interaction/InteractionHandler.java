package game.player.interaction;

import core.rendering_api.Input;
import core.utils.Vector3l;

import game.player.interaction.placeable_shapes.CubePlaceable;
import game.server.Game;
import game.settings.IntSettings;
import game.settings.KeySettings;
import game.settings.OptionSettings;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class InteractionHandler {

    /**
     * Handles input for active actions like locking position, setting start position, and using/destroying blocks.
     *
     * @param button the key or mouse button
     * @param action the action (press, release, repeat)
     */
    public void handleActiveInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.LOCK_PLACE_POSITION.keybind()) handleLockPlacePosition();
        if (action == GLFW_PRESS && button == KeySettings.SET_PLACE_START_POSITION.keybind()) handleSetPlaceStartPosition();
        if (action == GLFW_RELEASE && button == KeySettings.SET_PLACE_START_POSITION.keybind()) handleReleasePlaceStartPosition();

        if (button == KeySettings.DESTROY.keybind()) updateInfo(action, destroyInfo);
        if (button == KeySettings.USE.keybind()) updateInfo(action, useInfo);
    }

    /**
     * Handles input for inactive actions like changing break/place size and alignment.
     *
     * @param button the key or mouse button
     * @param action the action (press, release, repeat)
     */
    public static void handleInactiveInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(-1);
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(-1);
    }

    /**
     * Handles scroll input for shifting the locked or start position of a placement.
     *
     * @param yScroll the amount of scroll
     */
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

    /**
     * Updates the interaction handler's state for each game tick.
     */
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

    /**
     * Determines the current placing state based on the held item and target.
     *
     * @param currentTarget the current target of the player
     * @return the current {@code PlacingState}
     */
    public PlacingState getState(Target currentTarget) {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        boolean isLocked = lockedTarget != null;
        boolean isRepeat = startTarget != null;

        if (breakHeldOnly && !(placeable instanceof ShapePlaceable)) return PlacingState.NONE;
        if (placeable instanceof ChunkRebuildPlaceable) return PlacingState.NONE;
        if (placeable instanceof StructureSelector) return isLocked ? PlacingState.STRUCTURE_SELECT_LOCKED : PlacingState.STRUCTURE_SELECT;

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


    /**
     * Handles the "use" action for the held placeable.
     */
    private void handleUse() {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        if (placeable == null || OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY) {
            useInfo.lastAction = Game.getServer().getCurrentGameTick();
            useInfo.forceAction = false;
            return;
        }
        handleUseDestroy(useInfo, placeable, true);
    }

    /**
     * Handles the "destroy" action for the held placeable.
     */
    private void handleDestroy() {
        Placeable placeable = Game.getPlayer().getHeldPlaceable();
        if (placeable != null && !placeable.allowBreak() || OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY && !(placeable instanceof ShapePlaceable)) {
            destroyInfo.lastAction = Game.getServer().getCurrentGameTick();
            destroyInfo.forceAction = false;
            return;
        }
        if (!(placeable instanceof ShapePlaceable shapePlaceable)) placeable = new CubePlaceable(AIR).setBitMapToFull();
        else placeable = shapePlaceable.copyWithMaterial(AIR);
        handleUseDestroy(destroyInfo, placeable, false);
    }

    /**
     * Internal method to handle both "use" and "destroy" actions.
     *
     * @param info           the interaction info for the action
     * @param placeable      the placeable to use or destroy with
     * @param offsetPosition whether to offset the position based on the targeted side
     */
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
        if (placeable instanceof ShapePlaceable shapePlaceable) {
            if (startTarget != null) target = startTarget;
            Vector3l startPosition = offsetPosition ? target.offsetPosition() : target.position();
            placeable = new RepeatPlaceable(shapePlaceable, startPosition, position);
            targetedSide = target.side();
        }

        if (!Game.getServer().requestBreakPlaceInteraction(position, placeable, targetedSide)) return;
        info.lastAction = currentGameTick;
        startTarget = lockedTarget = null;
    }

    /**
     * Handles locking the currently targeted position.
     */
    private void handleLockPlacePosition() {
        Target currentTarget = Target.getPlayerTarget();
        if (currentTarget == null) return;
        PlacingState state = getState(currentTarget);
        if (state.isLocked()) lockedTarget = startTarget = null;
        else {
            lockedTarget = currentTarget;
            if (startTarget == null) startTarget = new Target(currentTarget);
        }
    }

    /**
     * Handles setting the start position for multi-block placement.
     */
    private void handleSetPlaceStartPosition() {
        Target currentTarget = Target.getPlayerTarget();
        PlacingState state = getState(currentTarget);
        if (state == PlacingState.NONE || state.isLocked()) return;
        startTarget = currentTarget;
    }

    /**
     * Handles releasing the start position for multi-block placement.
     */
    private void handleReleasePlaceStartPosition() {
        if (getState(Target.getPlayerTarget()).isLocked()) return;
        startTarget = null;
    }

    /**
     * Changes the size of the break/place area.
     *
     * @param addend the amount to change the size by
     */
    private static void changeBreakPlaceSize(int addend) {
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.clamp(IntSettings.BREAK_PLACE_SIZE.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.min(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldPlaceable() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
    }

    /**
     * Changes the alignment of the break/place area.
     *
     * @param addend the amount to change the alignment by
     */
    private static void changeBreakPlaceAlign(int addend) {
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.clamp(IntSettings.BREAK_PLACE_ALIGN.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.max(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
        if (Game.getPlayer().getHeldPlaceable() instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
    }

    /**
     * Updates the interaction info based on the button action.
     *
     * @param action the action (press or release)
     * @param info   the info to update
     */
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
