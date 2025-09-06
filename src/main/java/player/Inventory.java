package player;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import renderables.UiBackgroundElement;

public final class Inventory extends UiBackgroundElement {
    public Inventory() {
        super(new Vector2f(0.6f, 0.5f), new Vector2f(0.2f, 0.25f));
        setVisible(false);
        setAllowFocusScaling(false);
    }

    public void handleInput(int button, int action) {
        if (action != GLFW.GLFW_PRESS || !isVisible()) return;
    }
}
