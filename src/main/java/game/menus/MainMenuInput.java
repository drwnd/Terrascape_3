package game.menus;

import core.rendering_api.Input;

import static org.lwjgl.glfw.GLFW.*;

public final class MainMenuInput extends Input {
    /**
     * Constructs the input handler for the main menu.
     *
     * @param menu the main menu instance to handle input for
     */
    public MainMenuInput(MainMenu menu) {
        super(menu);
        this.menu = menu;
    }

    public float getScroll() {
        return scroll;
    }

    @Override
    public void setInputMode() {
        setStandardInputMode();
    }

    /**
     * Handles cursor movement on the main menu.
     * Updates hover states for menu elements.
     *
     * @param window the window handle
     * @param xPos   the new x-coordinate of the cursor
     * @param yPos   the new y-coordinate of the cursor
     */
    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
        menu.hoverOver(cursorPos);
    }

    /**
     * Handles mouse button clicks on the main menu.
     *
     * @param window the window handle
     * @param button the mouse button
     * @param action the action (press/release)
     * @param mods   bitfield for modifier keys
     */
    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action != GLFW_PRESS) return;

        menu.clickOn(cursorPos, button, action);
    }

    /**
     * Handles scroll wheel movement on the main menu.
     * Scrolls the list of world buttons.
     *
     * @param window  the window handle
     * @param xScroll the horizontal scroll amount
     * @param yScroll the vertical scroll amount
     */
    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        float newScroll = Math.max((float) (scroll - yScroll * 0.05), 0.0F);
        menu.moveWorldButtons(newScroll - scroll);
        scroll = newScroll;

        menu.hoverOver(cursorPos); // Fixes buttons being selected even if the cursor isn't hovered over them
    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {

    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final MainMenu menu;
    private float scroll = 0;
}