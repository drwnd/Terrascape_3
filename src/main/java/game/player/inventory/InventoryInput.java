package game.player.inventory;

import core.rendering_api.Input;

import game.server.Game;

import static org.lwjgl.glfw.GLFW.*;

public final class InventoryInput extends Input {

    public InventoryInput(Inventory inventory) {
        this.inventory = inventory;
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
        inventory.hoverOver(cursorPos);
        if (Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | IS_MOUSE_BUTTON))
            inventory.dragOver(cursorPos);
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
        inventory.clickOn(cursorPos, button, action);
        Game.getPlayer().handleInactiveKeyInput(button | Input.IS_MOUSE_BUTTON, action);
        inventory.handleInput(button | Input.IS_MOUSE_BUTTON, action, cursorPos);
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
        inventory.handleScroll(cursorPos, yScroll);
        inventory.hoverOver(cursorPos); // Fixes buttons being selected even if the cursor isn't hovered over them
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
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) Game.getPlayer().toggleInventory();
        Game.getPlayer().handleInactiveKeyInput(key, action);
        inventory.handleInput(key, action, cursorPos);
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final Inventory inventory;
    float structureScroll = 0, materialScroll = 0;
}
