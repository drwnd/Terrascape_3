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
     * Updates the cursor position and handles hover and drag events for the inventory.
     * @param window the window handle
     * @param xPos the new x-coordinate of the cursor
     * @param yPos the new y-coordinate of the cursor
     */
    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
        inventory.hoverOver(cursorPos);
        if (Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | IS_MOUSE_BUTTON))
            inventory.dragOver(cursorPos);
    }

    /**
     * Handles mouse button clicks, passing the event to the inventory and the player.
     * @param window the window handle
     * @param button the mouse button
     * @param action the action (press/release)
     * @param mods modification keys
     */
    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        inventory.clickOn(cursorPos, button, action);
        Game.getPlayer().handleInactiveKeyInput(button | Input.IS_MOUSE_BUTTON, action);
        inventory.handleInput(button | Input.IS_MOUSE_BUTTON, action, cursorPos);
    }

    /**
     * Handles scroll wheel input for the inventory tabs.
     * @param window the window handle
     * @param xScroll the horizontal scroll amount
     * @param yScroll the vertical scroll amount
     */
    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        inventory.handleScroll(cursorPos, yScroll);
        inventory.hoverOver(cursorPos); // Fixes buttons being selected even if the cursor isn't hovered over them
    }

    /**
     * Handles keyboard input, including closing the inventory with Escape.
     * @param window the window handle
     * @param key the keyboard key
     * @param scancode the system-specific scancode
     * @param action the action (press/release)
     * @param mods modification keys
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
