package game.player.interaction;

import core.rendering_api.Input;
import core.settings.FloatSetting;
import core.settings.KeySetting;

import game.server.Game;

import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class InteractionHandler {

    public void handleInput(int button, int action) {
        if (action == GLFW_PRESS && button == KeySetting.INCREASE_BREAK_PLACE_SIZE.keybind()) placeBreakSize = Math.min(CHUNK_SIZE_BITS, placeBreakSize + 1);
        if (action == GLFW_PRESS && button == KeySetting.DECREASE_BREAK_PLACE_SIZE.keybind()) placeBreakSize = Math.max(0, placeBreakSize - 1);
        if (action == GLFW_PRESS && button == KeySetting.LOCK_PLACE_POSITION.keybind()) startTarget = Target.getPlayerTarget();
        if (action == GLFW_RELEASE && button == KeySetting.LOCK_PLACE_POSITION.keybind()) startTarget = null;

        if (button == KeySetting.DESTROY.keybind()) updateInfo(action, destroyInfo);
        if (button == KeySetting.USE.keybind()) updateInfo(action, useInfo);
    }

    public void updateGameTick() {
        if (!Input.isKeyPressed(KeySetting.DESTROY)) updateInfo(GLFW_RELEASE, destroyInfo);
        if (!Input.isKeyPressed(KeySetting.USE)) updateInfo(GLFW_RELEASE, useInfo);

        handleDestroy();
        handleUse();
    }

    public int getPlaceBreakSize() {
        return placeBreakSize;
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
        handleUseDestroy(destroyInfo, new CubePlaceable(AIR), false);
    }

    private void handleUseDestroy(PlaceDestroyInfo info, Placeable placeable, boolean offsetPosition) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (!info.forceAction && (!info.buttonIsHeld || currentGameTick - info.lastAction < FloatSetting.BREAK_PLACE_INTERVALL.value())) return;
        info.forceAction = false;

        Target target = Target.getPlayerTarget();
        if (target == null) {
            info.lastAction = currentGameTick;
            info.forceAction = false;
            return;
        }

        Vector3i position = offsetPosition ? target.offsetPosition() : target.position();
        if (startTarget != null && placeable instanceof CubePlaceable cube) {
            Vector3i startPosition = offsetPosition ? startTarget.offsetPosition() : startTarget.position();
            placeable = new CuboidPlaceable(cube.getMaterial(), startPosition, position);
            startTarget = null;
        }
        if (Game.getServer().requestBreakPlaceInteraction(position, placeable)) info.lastAction = currentGameTick;
    }

    private static void updateInfo(int action, PlaceDestroyInfo info) {
        info.buttonIsHeld = action == GLFW_PRESS;
        if (info.buttonIsHeld) info.forceAction = true;
    }

    private final PlaceDestroyInfo useInfo = new PlaceDestroyInfo();
    private final PlaceDestroyInfo destroyInfo = new PlaceDestroyInfo();
    private Target startTarget = null;
    private int placeBreakSize = 4;

    private static class PlaceDestroyInfo {
        public long lastAction = 0;
        public boolean forceAction = false;
        public boolean buttonIsHeld = false;
    }
}
