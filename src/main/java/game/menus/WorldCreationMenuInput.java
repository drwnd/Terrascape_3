package game.menus;

import core.rendering_api.Input;

import static org.lwjgl.glfw.GLFW.*;

public final class WorldCreationMenuInput extends Input {

    public WorldCreationMenuInput(WorldCreationMenu menu) {
        super(menu);
        this.menu = menu;
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

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {

    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final WorldCreationMenu menu;
}
