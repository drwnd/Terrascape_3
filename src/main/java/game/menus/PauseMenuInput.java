package game.menus;

import core.rendering_api.Input;
import core.rendering_api.Window;

import game.server.Game;

import static org.lwjgl.glfw.GLFW.*;

public final class PauseMenuInput extends Input {

    /**
     * Constructs the input handler for the pause menu.
     *
     * @param menu the pause menu instance
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
     * Handles cursor movement in the pause menu.
     *
     * @param window the window handle
     * @param xPos   the x-coordinate
     * @param yPos   the y-coordinate
     */
    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);

        menu.hoverOver(cursorPos);
    }

    /**
     * Handles mouse clicks in the pause menu.
     *
     * @param window the window handle
     * @param button the mouse button
     * @param action the action
     * @param mods   modifiers
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
     * Handles keyboard input in the pause menu.
     * ESC key resumes the game.
     *
     * @param window   the window handle
     * @param key      the key code
     * @param scancode the scancode
     * @param action   the action
     * @param mods     modifiers
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
