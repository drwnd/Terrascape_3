package game.player.interaction;

import core.rendering_api.Input;
import core.utils.Vector3l;

import game.player.interaction.placeable_shapes.CubePlaceable;
import game.server.Game;
import game.settings.IntSettings;
import game.settings.KeySettings;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class InteractionHandler {

    public void handleInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_SIZE.keybind()) changeBreakPlaceSize(-1);
        if (action == GLFW_PRESS && button == KeySettings.INCREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(1);
        if (action == GLFW_PRESS && button == KeySettings.DECREASE_BREAK_PLACE_ALIGN.keybind()) changeBreakPlaceAlign(-1);
        if (action == GLFW_PRESS && button == KeySettings.LOCK_PLACE_POSITION.keybind()) startTarget = Target.getPlayerTarget();
        if (action == GLFW_RELEASE && button == KeySettings.LOCK_PLACE_POSITION.keybind()) startTarget = null;

        if (button == KeySettings.DESTROY.keybind()) updateInfo(action, destroyInfo);
        if (button == KeySettings.USE.keybind()) updateInfo(action, useInfo);
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
        if (!(placeable instanceof ShapePlaceable shapePlaceable)) placeable = new CubePlaceable(AIR);
        else placeable = shapePlaceable.copyWithMaterial(AIR);
        handleUseDestroy(destroyInfo, placeable, false);
    }

    private void handleUseDestroy(PlaceDestroyInfo info, Placeable placeable, boolean offsetPosition) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (!info.forceAction && (!info.buttonIsHeld || currentGameTick - info.lastAction < IntSettings.BREAK_PLACE_INTERVALL.value())) return;
        info.forceAction = false;

        Target target = Target.getPlayerTarget();
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
        if (Game.getServer().requestBreakPlaceInteraction(position, placeable, targetedSide)) info.lastAction = currentGameTick;
    }

    private static void changeBreakPlaceSize(int addend) {
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.clamp(IntSettings.BREAK_PLACE_SIZE.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.min(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
    }

    private static void changeBreakPlaceAlign(int addend) {
        IntSettings.BREAK_PLACE_ALIGN.setValue(Math.clamp(IntSettings.BREAK_PLACE_ALIGN.value() + addend, 0, CHUNK_SIZE_BITS + 2));
        IntSettings.BREAK_PLACE_SIZE.setValue(Math.max(IntSettings.BREAK_PLACE_SIZE.value(), IntSettings.BREAK_PLACE_ALIGN.value()));
    }

    private static void updateInfo(int action, PlaceDestroyInfo info) {
        info.buttonIsHeld = action == GLFW_PRESS;
        if (info.buttonIsHeld) info.forceAction = true;
    }

    private final PlaceDestroyInfo useInfo = new PlaceDestroyInfo();
    private final PlaceDestroyInfo destroyInfo = new PlaceDestroyInfo();
    private Target startTarget = null;

    private static class PlaceDestroyInfo {
        public long lastAction = 0;
        public boolean forceAction = false;
        public boolean buttonIsHeld = false;
    }
}
