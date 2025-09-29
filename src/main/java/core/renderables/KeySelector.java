package core.renderables;

import core.assets.identifiers.TextureIdentifier;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.settings.KeySetting;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public final class KeySelector extends UiButton {

    public KeySelector(Vector2f sizeToParent, Vector2f offsetToParent, KeySetting setting) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());
        this.setting = setting;

        UiElement blackBox = new UiElement(new Vector2f(0.5f, 0.6f), new Vector2f(0.45f, 0.2f), TextureIdentifier.INVENTORY_OVERLAY);
        display = new TextElement(new Vector2f(0.05f, 0.5f), getDisplayString(value));
        blackBox.addRenderable(display);

        addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), setting.name()));
        addRenderable(blackBox);

        matchSetting();
    }

    public void setToDefault() {
        setValue(setting.defaultValue());
    }

    public KeySetting getSetting() {
        return setting;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int key) {
        value = key;
        display.setText(getDisplayString(value));
    }

    public void matchSetting() {
        setValue(setting.value());
    }


    private static String getDisplayString(int value) {
        return switch (value) {
            case GLFW.GLFW_KEY_UNKNOWN -> "None";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Control";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Control";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW.GLFW_KEY_UP -> "Up Arrow";
            case GLFW.GLFW_KEY_DOWN -> "Down Arrow";
            case GLFW.GLFW_KEY_LEFT -> "Left Arrow";
            case GLFW.GLFW_KEY_RIGHT -> "Right Arrow";

            case GLFW.GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON -> "Left Click";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT | Input.IS_MOUSE_BUTTON -> "Right Click";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE | Input.IS_MOUSE_BUTTON -> "Middle Click";
            case GLFW.GLFW_MOUSE_BUTTON_4 | Input.IS_MOUSE_BUTTON -> "Mouse Button 4";
            case GLFW.GLFW_MOUSE_BUTTON_5 | Input.IS_MOUSE_BUTTON -> "Mouse Button 5";
            case GLFW.GLFW_MOUSE_BUTTON_6 | Input.IS_MOUSE_BUTTON -> "Mouse Button 6";
            case GLFW.GLFW_MOUSE_BUTTON_7 | Input.IS_MOUSE_BUTTON -> "Mouse Button 7";
            case GLFW.GLFW_MOUSE_BUTTON_8 | Input.IS_MOUSE_BUTTON -> "Mouse Button 8";

            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";

            default -> {
                if (Character.isDefined(value)) yield Character.toString(value);
                yield String.valueOf(value);
            }
        };
    }

    private Clickable getAction() {
        return (Vector2i cursorPos, int button, int action) -> {
            if (action != GLFW.GLFW_PRESS) return;
            Window.setInput(new KeySelectorInput(this));
        };
    }

    private final KeySetting setting;
    private final TextElement display;
    private int value;
}
