package game.player.inventory;

import core.rendering_api.Input;

import core.rendering_api.MenuInput;
import game.server.Game;
import core.utils.MainThread;

import static org.lwjgl.glfw.GLFW.*;

public final class InventoryInput extends MenuInput<Inventory> {

    @MainThread
    public InventoryInput(Inventory inventory) {
        super(inventory, null, () -> inventory.getMaxScroll(Input.getCursorPos()));
    }

    @Override
    @MainThread
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        menu.clickOn(cursorPos, button, action);
        Game.getPlayer().handleInactiveKeyInput(button | Input.IS_MOUSE_BUTTON, action);
        menu.handleInput(button | Input.IS_MOUSE_BUTTON, action, cursorPos);
    }

    @Override
    @MainThread
    public void scrollCallback(long window, double xScroll, double yScroll) {
        menu.handleScroll(cursorPos, yScroll);
        menu.hoverOver(cursorPos);
    }

    @Override
    @MainThread
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) Game.getPlayer().toggleInventory();
        Game.getPlayer().handleInactiveKeyInput(key, action);
        menu.handleInput(key, action, cursorPos);
    }

    @Override
    @MainThread
    public void setScroll(float scroll) {
        super.setScroll(scroll);
    }

    @Override
    @MainThread
    public float getScroll() {
        return super.getScroll();
    }

    float structureScroll = 0, materialScroll = 0;
}
