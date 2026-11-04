package game.player.interaction;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class PlaceDestroyInfo {
    public long lastAction = 0;
    public boolean forceAction = false;
    public boolean buttonIsHeld = false;

    public void setUsed(long currentGameTick) {
        forceAction = false;
        lastAction = currentGameTick;
    }

    public void update(int action) {
        buttonIsHeld = action == GLFW_PRESS;
        if (buttonIsHeld) forceAction = true;
    }
}
