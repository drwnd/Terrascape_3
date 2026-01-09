package core.renderables;

import core.assets.CoreTextures;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.settings.KeyBound;
import core.utils.Message;
import core.utils.StringGetter;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public final class KeySelector extends UiButton {

    public KeySelector(Vector2f sizeToParent, Vector2f offsetToParent, KeyBound setting, StringGetter settingName) {
        super(sizeToParent, offsetToParent);
        setAction(getAction());
        this.setting = setting;

        UiElement blackBox = new UiElement(new Vector2f(0.5F, 0.6F), new Vector2f(0.45F, 0.2F), CoreTextures.OVERLAY);
        display = new TextElement(new Vector2f(0.05F, 0.5F), new Message(getDisplayString(value)));
        blackBox.addRenderable(display);

        addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), settingName));
        addRenderable(blackBox);

        matchSetting();
    }

    public void setToDefault() {
        setValue(setting.defaultKeybind());
    }

    public KeyBound getSetting() {
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
        setValue(setting.keybind());
    }


    public static String getDisplayString(int value) {
        return switch (value) {
            case GLFW_KEY_UNKNOWN -> "None";
            case GLFW_KEY_TAB -> "Tab";
            case GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW_KEY_SPACE -> "Space";
            case GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW_KEY_LEFT_CONTROL -> "Left Control";
            case GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW_KEY_RIGHT_CONTROL -> "Right Control";
            case GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW_KEY_UP -> "Up Arrow";
            case GLFW_KEY_DOWN -> "Down Arrow";
            case GLFW_KEY_LEFT -> "Left Arrow";
            case GLFW_KEY_RIGHT -> "Right Arrow";

            case GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON -> "Left Click";
            case GLFW_MOUSE_BUTTON_RIGHT | Input.IS_MOUSE_BUTTON -> "Right Click";
            case GLFW_MOUSE_BUTTON_MIDDLE | Input.IS_MOUSE_BUTTON -> "Middle Click";
            case GLFW_MOUSE_BUTTON_4 | Input.IS_MOUSE_BUTTON -> "Mouse Button 4";
            case GLFW_MOUSE_BUTTON_5 | Input.IS_MOUSE_BUTTON -> "Mouse Button 5";
            case GLFW_MOUSE_BUTTON_6 | Input.IS_MOUSE_BUTTON -> "Mouse Button 6";
            case GLFW_MOUSE_BUTTON_7 | Input.IS_MOUSE_BUTTON -> "Mouse Button 7";
            case GLFW_MOUSE_BUTTON_8 | Input.IS_MOUSE_BUTTON -> "Mouse Button 8";

            case GLFW_KEY_F1 -> "F1";
            case GLFW_KEY_F2 -> "F2";
            case GLFW_KEY_F3 -> "F3";
            case GLFW_KEY_F4 -> "F4";
            case GLFW_KEY_F5 -> "F5";
            case GLFW_KEY_F6 -> "F6";
            case GLFW_KEY_F7 -> "F7";
            case GLFW_KEY_F8 -> "F8";
            case GLFW_KEY_F9 -> "F9";
            case GLFW_KEY_F10 -> "F10";
            case GLFW_KEY_F11 -> "F11";
            case GLFW_KEY_F12 -> "F12";

            default -> {
                if (Character.isDefined(value)) yield Character.toString(value);
                yield String.valueOf(value);
            }
        };
    }

    private Clickable getAction() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            Window.setInput(new KeySelectorInput(this));
        };
    }

    private final KeyBound setting;
    private final TextElement display;
    private int value;
}
