package game.menus;

import core.rendering_api.Input;
import core.rendering_api.Window;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class StructurePreviewMenuInput extends Input {

    public StructurePreviewMenuInput(StructurePreviewMenu menu) {
        super(menu);
        this.menu = menu;
    }

    public Vector2i getCursorMovement() {
        Vector2i movement = new Vector2i(cursorPos).sub(lastCursorPos);
        lastCursorPos.set(cursorPos);
        return movement;
    }

    @Override
    public void setInputMode() {
        setStandardInputMode();
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
        menu.hoverOver(cursorPos);
        if (Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | IS_MOUSE_BUTTON))
            menu.dragOver(cursorPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        menu.clickOn(cursorPos, button, action);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {
        menu.changeZoom(yScroll > 0);
    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) Window.popRenderable();
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final StructurePreviewMenu menu;
    private final Vector2i lastCursorPos = new Vector2i();
}
