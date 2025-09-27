package rendering_api;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import renderables.Renderable;
import settings.KeySetting;

public abstract class Input {

    public static final int IS_MOUSE_BUTTON = 0x80000000;
    public static final int BUTTON_MASK = 0x7FFFFFFF;

    protected final Vector2i cursorPos = new Vector2i();

    public Input() {
        // This needs to exist so subclasses don't have to call the other constructor
    }

    public Input(Renderable menu) {
        cursorPos.set(getCursorPos());
        menu.hoverOver(cursorPos);
    }

    public static boolean isKeyPressed(int keycode) {
        if (keycode == GLFW.GLFW_KEY_UNKNOWN) return false;
        if ((keycode & Input.IS_MOUSE_BUTTON) == 0)
            return GLFW.glfwGetKey(Window.getWindow(), keycode & Input.BUTTON_MASK) == GLFW.GLFW_PRESS;
        else
            return GLFW.glfwGetMouseButton(Window.getWindow(), keycode & Input.BUTTON_MASK) == GLFW.GLFW_PRESS;
    }

    public static boolean isKeyPressed(KeySetting setting) {
        return isKeyPressed(setting.value());
    }

    public static Vector2i getCursorPos() {
        double[] cursorX = new double[1];
        double[] cursorY = new double[1];
        GLFW.glfwGetCursorPos(Window.getWindow(), cursorX, cursorY);
        return new Vector2i((int) cursorX[0], Window.getHeight() - (int) cursorY[0]);
    }

    public static void setStandardInputMode() {
        GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    public void standardCursorPosCallBack(double xPos, double yPos) {
        cursorPos.set((int) xPos, Window.getHeight() - (int) yPos);
    }

    public abstract void setInputMode();

    public abstract void cursorPosCallback(long window, double xPos, double yPos);

    public abstract void mouseButtonCallback(long window, int button, int action, int mods);

    public abstract void scrollCallback(long window, double xScroll, double yScroll);

    public abstract void keyCallback(long window, int key, int scancode, int action, int mods);

    public abstract void charCallback(long window, int codePoint);
}
