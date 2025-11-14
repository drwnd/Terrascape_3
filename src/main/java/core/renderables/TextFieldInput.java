package core.renderables;

import core.rendering_api.Input;

import core.rendering_api.Window;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public class TextFieldInput extends Input {

    public TextFieldInput(TextField field) {
        super(field);
        this.field = field;
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
        if (action != GLFW.GLFW_PRESS) return;
        if (!field.containsPixelCoordinate(cursorPos)) unselect();
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return;
        if (key == GLFW.GLFW_KEY_ESCAPE) unselect();
        if (key == GLFW.GLFW_KEY_BACKSPACE) handleBackspace();
    }

    @Override
    public void charCallback(long window, int codePoint) {
        char[] chars = Character.toChars(codePoint);
        field.setText(field.getText() + toString(chars));
    }


    private void unselect() {
        Renderable renderable = Window.topRenderable();
        renderable.setOnTop();
        renderable.hoverOver(cursorPos);
    }

    protected void handleBackspace() {
        String currentText = field.getText();
        if (currentText.isEmpty()) return;

        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            int spaceIndex = currentText.lastIndexOf(' ');
            if (spaceIndex == -1) field.setText("");
            else field.setText(currentText.substring(0, spaceIndex));
        } else field.setText(currentText.substring(0, currentText.length() - 1));
    }

    private String toString(char[] chars) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) stringBuilder.append(c);
        return stringBuilder.toString();
    }

    protected final TextField field;
    private final Vector2i cursorPos = new Vector2i();
}
