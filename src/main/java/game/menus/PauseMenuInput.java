package game.menus;

import core.rendering_api.Input;
import core.rendering_api.Window;

import game.server.Game;

import static org.lwjgl.glfw.GLFW.*;

public final class PauseMenuInput extends Input {

/**
 * Creates a new PauseMenuInput instance.
 *
 * @param menu parameter
 */
    public PauseMenuInput(PauseMenu menu) {
        super(menu);
        this.menu = menu;
    }


    @Override
    public void setInputMode() {
        setStandardInputMode();
    }

/**
 * Performs cursor pos callback.
 *
 * @param window parameter
 * @param xPos parameter
 * @param yPos parameter
 */
    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);

        menu.hoverOver(cursorPos);
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
        if (action != GLFW_PRESS) return;

        menu.clickOn(cursorPos, button, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

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
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            Window.popRenderable();
            Game.getServer().startTicks();
        }
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final PauseMenu menu;
}
