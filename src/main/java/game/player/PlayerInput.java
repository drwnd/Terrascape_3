package game.player;

import core.rendering_api.Input;
import core.rendering_api.Window;
import core.settings.ToggleSetting;

import game.menus.PauseMenu;
import game.server.Game;

import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class PlayerInput extends Input {
    @Override
    public void setInputMode() {
        cursorPos.set(getCursorPos());
        lastCursorPos.set(cursorPos);
        glfwSetInputMode(Window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported()) {
            if (ToggleSetting.RAW_MOUSE_INPUT.value())
                glfwSetInputMode(Window.getWindow(), GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            else
                glfwSetInputMode(Window.getWindow(), GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
        }
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        Game.getPlayer().handleActiveButtonInput(button | Input.IS_MOUSE_BUTTON, action);
        Game.getPlayer().handleInactiveKeyInput(button | Input.IS_MOUSE_BUTTON, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        Game.getPlayer().handleInactiveScrollInput(xScroll, yScroll);
    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) Window.pushRenderable(new PauseMenu());
        Game.getPlayer().handleActiveButtonInput(key, action);
        Game.getPlayer().handleInactiveKeyInput(key, action);
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    public Vector2i getCursorMovement() {
        Vector2i movement = new Vector2i(cursorPos).sub(lastCursorPos);
        lastCursorPos.set(cursorPos);
        return movement;
    }

    private final Vector2i lastCursorPos = new Vector2i();
}
