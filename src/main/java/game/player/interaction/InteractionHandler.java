package game.player.interaction;

import core.rendering_api.Input;
import core.settings.FloatSetting;
import core.settings.KeySetting;

import game.server.Game;

import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

import static game.utils.Constants.*;

public final class InteractionHandler {

    public void handleInput(int button, int action) {
        if (action == GLFW.GLFW_PRESS && button == KeySetting.INCREASE_BREAK_PLACE_SIZE.value()) placeBreakSize = Math.min(CHUNK_SIZE_BITS, placeBreakSize + 1);
        if (action == GLFW.GLFW_PRESS && button == KeySetting.DECREASE_BREAK_PLACE_SIZE.value()) placeBreakSize = Math.max(0, placeBreakSize - 1);

        if (button == KeySetting.DESTROY.value()) updateInfo(action, destroyInfo);
        if (button == KeySetting.USE.value()) updateInfo(action, useInfo);
    }

    public void updateGameTick() {
        if (!Input.isKeyPressed(KeySetting.DESTROY)) updateInfo(GLFW.GLFW_RELEASE, destroyInfo);
        if (!Input.isKeyPressed(KeySetting.USE)) updateInfo(GLFW.GLFW_RELEASE, useInfo);

        handleDestroy();
        handleUse();
    }


    private void handleUse() {
        byte material = Game.getPlayer().getHeldMaterial();
        if (material == AIR) return;
        handleUseDestroy(useInfo, material, true);
    }

    private void handleDestroy() {
        handleUseDestroy(destroyInfo, AIR, false);
    }

    private void handleUseDestroy(PlaceDestroyInfo info, byte material, boolean offsetPosition) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        if (!info.forceAction && (!info.buttonIsHeld || currentGameTick - info.lastAction < FloatSetting.BREAK_PLACE_INTERVALL.value())) return;
        info.forceAction = false;

        Target target = Target.getPlayerTarget();
        if (target == null) return;

        Vector3i position = offsetPosition ? target.offsetPosition() : target.position();
        if (Game.getServer().requestBreakPlaceInteraction(position, placeBreakSize, material)) info.lastAction = currentGameTick;
    }

    private void updateInfo(int action, PlaceDestroyInfo info) {
        info.buttonIsHeld = action == GLFW.GLFW_PRESS;
        if (info.buttonIsHeld) info.forceAction = true;
    }

    private final PlaceDestroyInfo useInfo = new PlaceDestroyInfo();
    private final PlaceDestroyInfo destroyInfo = new PlaceDestroyInfo();
    private int placeBreakSize = 4;

    private static class PlaceDestroyInfo {
        public long lastAction = 0;
        public boolean forceAction = false;
        public boolean buttonIsHeld = false;
    }
}
