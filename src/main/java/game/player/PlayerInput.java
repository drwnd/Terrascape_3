package game.player;

import core.rendering_api.Input;
import core.rendering_api.Window;
import core.settings.ToggleSetting;

import game.menus.PauseMenu;
import game.server.Game;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class PlayerInput extends Input {
    @Override
    public void setInputMode() {
        cursorPos.set(getCursorPos());
        lastCursorPos.set(cursorPos);
        GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        if (GLFW.glfwRawMouseMotionSupported()) {
            if (ToggleSetting.RAW_MOUSE_INPUT.value())
                GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
            else
                GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_FALSE);
        }
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        Game.getPlayer().handleActiveInput(button | Input.IS_MOUSE_BUTTON, action);
        Game.getPlayer().handleInactiveInput(button | Input.IS_MOUSE_BUTTON, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        if (ToggleSetting.SCROLL_HOTBAR.value()) {
            Hotbar hotbar = Game.getPlayer().getHotbar();
            hotbar.setSelectedSlot(hotbar.getSelectedSlot() + (yScroll < 0.0 ? 1 : -1));
        }
    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) Window.pushRenderable(new PauseMenu());
        Game.getPlayer().handleActiveInput(key, action);
        Game.getPlayer().handleInactiveInput(key, action);
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
