package core.renderables;

import core.rendering_api.Input;

import core.rendering_api.Window;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class TextFieldInput extends Input {

    public TextFieldInput(TextField field) {
        super(field);
        this.field = field;
    }

    @Override
    public void setInputMode() {
        setStandardInputMode();
        cursorIndex = field.getText().length();
    }

    @Override
    public void cursorPosCallback(long window, double xPos, double yPos) {
        standardCursorPosCallBack(xPos, yPos);
    }

    @Override
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action != GLFW_PRESS) return;
        if (!field.containsPixelCoordinate(cursorPos)) unselect();
    }

    @Override
    public void scrollCallback(long window, double xScroll, double yScroll) {

    }

    @Override
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
        if (key == GLFW_KEY_ESCAPE) unselect();
        if (key == GLFW_KEY_BACKSPACE) handleBackspace();
        if (key == GLFW_KEY_LEFT) handleMoveLeft();
        if (key == GLFW_KEY_RIGHT) handleMoveRight();
    }

    @Override
    public void charCallback(long window, int codePoint) {
        char[] chars = Character.toChars(codePoint);
        field.setText(insert(field.getText(), toString(chars)));
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    private void unselect() {
        Renderable renderable = Window.topRenderable();
        renderable.setOnTop();
        renderable.hoverOver(cursorPos);
    }

    protected void handleBackspace() {
        String currentText = field.getText();
        if (currentText.isEmpty()) return;

        if (Input.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            int spaceIndex = lastSpaceIndexBeforeCursor(currentText);
            field.setText(cut(currentText, spaceIndex));
        } else field.setText(cut(currentText));
    }

    protected void handleMoveLeft() {
        if (Input.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW_KEY_RIGHT_CONTROL))
            cursorIndex = Math.clamp(lastSpaceIndexBeforeCursor(field.getText()), 0, cursorIndex);
        else cursorIndex = Math.clamp(cursorIndex - 1, 0, field.getText().length());
    }

    protected void handleMoveRight() {
        if (Input.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW_KEY_RIGHT_CONTROL))
            cursorIndex = Math.clamp(firstSpaceIndexAfterCursor(field.getText()), cursorIndex, field.getText().length());
        else cursorIndex = Math.clamp(cursorIndex + 1, 0, field.getText().length());
    }

    private String insert(String string, String toInsert) {
        String prefix = string.substring(0, cursorIndex);
        String suffix = string.substring(cursorIndex);
        cursorIndex += toInsert.length();

        return prefix + toInsert + suffix;
    }

    private String cut(String string, int spaceIndex) {
        if (spaceIndex == -1) {
            String suffix = string.substring(cursorIndex);
            cursorIndex = 0;
            return suffix;
        }

        String prefix = string.substring(0, spaceIndex);
        String suffix = string.substring(cursorIndex);
        String result = prefix + suffix;
        cursorIndex -= string.length() - result.length();

        return result;
    }

    private String cut(String string) {
        if (cursorIndex == 0) return string;

        String prefix = string.substring(0, cursorIndex - 1);
        String suffix = string.substring(cursorIndex);
        cursorIndex = Math.clamp(cursorIndex - 1, 0, field.getText().length());

        return prefix + suffix;
    }

    private int lastSpaceIndexBeforeCursor(String string) {
        char[] chars = string.toCharArray();
        for (int index = cursorIndex - 1; index >= 0; index--) if (chars[index] == ' ') return index;
        return -1;
    }

    private int firstSpaceIndexAfterCursor(String string) {
        char[] chars = string.toCharArray();
        for (int index = cursorIndex + 1; index < chars.length; index++) if (chars[index] == ' ') return index;
        return chars.length;
    }

    private static String toString(char[] chars) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) stringBuilder.append(c);
        return stringBuilder.toString();
    }

    protected final TextField field;
    protected int cursorIndex;
    private final Vector2i cursorPos = new Vector2i();
}
