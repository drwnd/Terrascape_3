package game.player;

import core.rendering_api.Input;
import core.rendering_api.Window;
import core.settings.CoreToggleSettings;

import game.menus.PauseMenu;

import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class PlayerInput extends Input {

    public PlayerInput(Player player) {
        this.player = player;
    }

/**
 * Sets input mode.
 */
    @Override
    public void setInputMode() {
        cursorPos.set(getCursorPos());
        lastCursorPos.set(cursorPos);
        glfwSetInputMode(Window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported()) {
            if (CoreToggleSettings.RAW_MOUSE_INPUT.value())
                glfwSetInputMode(Window.getWindow(), GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            else
                glfwSetInputMode(Window.getWindow(), GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
        }
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
    }

/**
 * Performs mouse button callback.
 *
 * @param window parameter
 * @param button parameter
 * @param action parameter
 * @param mods parameter
 */
    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
       player.handleActiveButtonInput(button | Input.IS_MOUSE_BUTTON, action);
       player.handleInactiveKeyInput(button | Input.IS_MOUSE_BUTTON, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
       player.handleScrollInput(yScroll);
    }

/**
 * Performs key callback.
 *
 * @param window parameter
 * @param key Y coordinate in local block coordinates
 * @param scancode parameter
 * @param action parameter
 * @param mods parameter
 */
    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) Window.pushRenderable(new PauseMenu());
       player.handleActiveButtonInput(key, action);
       player.handleInactiveKeyInput(key, action);
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

/**
 * Returns the cursor movement.
 * @return result
 */
    public Vector2i getCursorMovement() {
        Vector2i movement = new Vector2i(cursorPos).sub(lastCursorPos);
        lastCursorPos.set(cursorPos);
        return movement;
    }

    private final Vector2i lastCursorPos = new Vector2i();
    private final Player player;
}
