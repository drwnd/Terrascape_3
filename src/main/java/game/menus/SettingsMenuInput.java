package game.menus;

import core.rendering_api.Input;
import core.rendering_api.Window;

import static org.lwjgl.glfw.GLFW.*;

public final class SettingsMenuInput extends Input {

/**
 * Creates a new SettingsMenuInput instance.
 *
 * @param menu parameter
 */
    public SettingsMenuInput(SettingsMenu menu) {
        super(menu);
        this.menu = menu;
    }

    public float getScroll() {
        return scroll;
    }

    public void setScroll(float scroll) {
        this.scroll = scroll;
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

/**
 * Performs scroll callback.
 *
 * @param window parameter
 * @param xScroll parameter
 * @param yScroll parameter
 */
    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        float newScroll = Math.max((float) (scroll - yScroll * 0.05), 0.0F);
        menu.scrollSectionButtons(newScroll - scroll);
        scroll = newScroll;

        menu.hoverOver(cursorPos); // Fixes buttons being selected even if the cursor isn't hovered over them
    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) Window.popRenderable();
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final SettingsMenu menu;
    private float scroll = 0;
}
