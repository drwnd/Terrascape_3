package core.renderables;

import core.rendering_api.Input;

import static org.lwjgl.glfw.GLFW.*;

public final class KeySelectorInput extends Input {

    public KeySelectorInput(KeySelector selector) {
        super(selector);
        this.selector = selector;
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
        if (action != GLFW_PRESS) return;
        selector.setValue(button | Input.IS_MOUSE_BUTTON);
        selector.getParent().setOnTop();
        selector.getParent().hoverOver(cursorPos);
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action != GLFW_PRESS) return;
        if (key == GLFW_KEY_ESCAPE) selector.setValue(GLFW_KEY_UNKNOWN);
        else selector.setValue(key);
        selector.getParent().setOnTop();
        selector.getParent().hoverOver(cursorPos);
    }

    @Override
    public void charCallback(long window, int codePoint) {

    }

    private final KeySelector selector;
}
