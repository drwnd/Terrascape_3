package game.menus;

import core.rendering_api.Input;

import static org.lwjgl.glfw.GLFW.*;

public final class WorldCreationMenuInput extends Input {

/**
 * Creates a new WorldCreationMenuInput instance.
 *
 * @param menu parameter
 */
    public WorldCreationMenuInput(WorldCreationMenu menu) {
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

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {

    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final WorldCreationMenu menu;
}
