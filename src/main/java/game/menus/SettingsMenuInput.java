package game.menus;

import core.rendering_api.Input;
import core.rendering_api.Window;

import static org.lwjgl.glfw.GLFW.*;

public final class SettingsMenuInput extends Input {

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
     * Updates the cursor position and informs the menu about hover events.
     *
     * @param window the window that received the event
     * @param xPos the new x-coordinate of the cursor, in screen coordinates
     * @param yPos the new y-coordinate of the cursor, in screen coordinates
     */
    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
        menu.hoverOver(cursorPos);
    }

    /**
     * Handles mouse button clicks and informs the menu.
     *
     * @param window the window that received the event
     * @param button the mouse button that was clicked
     * @param action the action (press, release, repeat)
     * @param mods bitfield describing which modifier keys were held down
     */
    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action != GLFW_PRESS) return;

        menu.clickOn(cursorPos, button, action);
    }

    /**
     * Handles scroll events, updates the scroll position, and scrolls the menu's section buttons.
     *
     * @param window the window that received the event
     * @param xScroll the scroll offset along the x-axis
     * @param yScroll the scroll offset along the y-axis
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
