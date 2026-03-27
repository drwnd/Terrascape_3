package game.player.inventory;

import core.rendering_api.Input;

import core.rendering_api.Window;
import game.server.Game;

import static org.lwjgl.glfw.GLFW.*;

public final class InventoryInput extends Input {

    public InventoryInput(Inventory inventory) {
        this.inventory = inventory;
    }

    public float getStructureScroll() {
        return structureScroll;
    }

    public float getMaterialScroll() {
        return materialScroll;
    }

    @Override
    public void setInputMode() {
        setStandardInputMode();
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
        inventory.hoverOver(cursorPos);
        if (Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | IS_MOUSE_BUTTON))
            inventory.dragOver(cursorPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        inventory.clickOn(cursorPos, button, action);
        Game.getPlayer().handleInactiveKeyInput(button | Input.IS_MOUSE_BUTTON, action);
        inventory.handleInput(button | Input.IS_MOUSE_BUTTON, action, cursorPos);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        if (cursorPos.x < Window.getWidth() / 3) {
            float newScroll = Math.max((float) (structureScroll - yScroll * 0.05), 0.0F);
            inventory.structureTab.moveStructureButtons(newScroll - structureScroll);
            structureScroll = newScroll;
        } else if (cursorPos.x > Window.getWidth() * 2 / 3) {
            float newScroll = Math.max((float) (materialScroll - yScroll * 0.05), 0.0F);
            inventory.placeablesTab.moveMaterialButtons(newScroll - materialScroll);
            materialScroll = newScroll;
        } else return;

        inventory.hoverOver(cursorPos); // Fixes buttons being selected even if the cursor isn't hovered over them
    }

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
    private float structureScroll = 0, materialScroll =  0;
}
