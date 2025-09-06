package player;

import rendering_api.Input;
import server.Game;

public final class InventoryInput extends Input {

    public InventoryInput(Inventory inventory) {
        super(inventory);
        this.inventory = inventory;
    }

    @Override
    public void setInputMode() {
        setStandardInputMode();
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        inventory.clickOn(cursorPos, button, action);
        Game.getPlayer().handleInactiveInput(button | Input.IS_MOUSE_BUTTON, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        Game.getPlayer().handleInactiveInput(key, action);
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final Inventory inventory;
}
