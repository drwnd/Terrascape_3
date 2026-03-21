package game.settings;

import core.rendering_api.Input;
import core.settings.KeySetting;

import static org.lwjgl.glfw.GLFW.*;

public enum KeySettings implements KeySetting {
    MOVE_FORWARD(GLFW_KEY_W),
    MOVE_BACK(GLFW_KEY_S),
    MOVE_RIGHT(GLFW_KEY_D),
    MOVE_LEFT(GLFW_KEY_A),
    JUMP(GLFW_KEY_SPACE),
    SPRINT(GLFW_KEY_LEFT_CONTROL),
    SNEAK(GLFW_KEY_LEFT_SHIFT),
    CRAWL(GLFW_KEY_CAPS_LOCK),
    FLY_FAST(GLFW_KEY_TAB),
    HOTBAR_SLOT_1(GLFW_KEY_1),
    HOTBAR_SLOT_2(GLFW_KEY_2),
    HOTBAR_SLOT_3(GLFW_KEY_3),
    HOTBAR_SLOT_4(GLFW_KEY_4),
    HOTBAR_SLOT_5(GLFW_KEY_5),
    HOTBAR_SLOT_6(GLFW_KEY_6),
    HOTBAR_SLOT_7(GLFW_KEY_7),
    HOTBAR_SLOT_8(GLFW_KEY_8),
    HOTBAR_SLOT_9(GLFW_KEY_9),
    DESTROY(GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON),
    USE(GLFW_MOUSE_BUTTON_RIGHT | Input.IS_MOUSE_BUTTON),
    PICK_BLOCK(GLFW_MOUSE_BUTTON_MIDDLE | Input.IS_MOUSE_BUTTON),
    INVENTORY(GLFW_KEY_E),
    ZOOM(GLFW_KEY_V),
    INCREASE_BREAK_PLACE_SIZE(GLFW_KEY_UP),
    DECREASE_BREAK_PLACE_SIZE(GLFW_KEY_DOWN),
    INCREASE_BREAK_PLACE_ALIGN(GLFW_KEY_RIGHT),
    DECREASE_BREAK_PLACE_ALIGN(GLFW_KEY_LEFT),
    DROP(GLFW_KEY_Q),
    OPEN_CHAT(GLFW_KEY_T),
    START_COMMAND(GLFW_KEY_H),
    RELOAD_MATERIALS(GLFW_KEY_F6),
    GET_CHUNK_REBUILD_PLACEABLE(GLFW_KEY_F5),
    LOCK_PLACE_POSITION(GLFW_KEY_TAB),
    ROTATE_SHAPE_FORWARD(GLFW_KEY_F1),
    ROTATE_SHAPE_BACKWARD(GLFW_KEY_F2);

    KeySettings(int defaultValue) {
        this.defaultKeybind = defaultValue;
        this.keybind = defaultValue;
    }


    @Override
    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    @Override
    public int keybind() {
        return keybind;
    }

    @Override
    public int defaultKeybind() {
        return defaultKeybind;
    }

    private final int defaultKeybind;
    private int keybind;
}
