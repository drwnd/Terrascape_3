package core.settings;

import core.rendering_api.Input;

import org.lwjgl.glfw.GLFW;

public enum KeySetting {

    MOVE_FORWARD(GLFW.GLFW_KEY_W),
    MOVE_BACK(GLFW.GLFW_KEY_S),
    MOVE_RIGHT(GLFW.GLFW_KEY_D),
    MOVE_LEFT(GLFW.GLFW_KEY_A),
    JUMP(GLFW.GLFW_KEY_SPACE),
    SPRINT(GLFW.GLFW_KEY_LEFT_CONTROL),
    SNEAK(GLFW.GLFW_KEY_LEFT_SHIFT),
    CRAWL(GLFW.GLFW_KEY_CAPS_LOCK),
    FLY_FAST(GLFW.GLFW_KEY_TAB),
    HOTBAR_SLOT_1(GLFW.GLFW_KEY_1),
    HOTBAR_SLOT_2(GLFW.GLFW_KEY_2),
    HOTBAR_SLOT_3(GLFW.GLFW_KEY_3),
    HOTBAR_SLOT_4(GLFW.GLFW_KEY_4),
    HOTBAR_SLOT_5(GLFW.GLFW_KEY_5),
    HOTBAR_SLOT_6(GLFW.GLFW_KEY_6),
    HOTBAR_SLOT_7(GLFW.GLFW_KEY_7),
    HOTBAR_SLOT_8(GLFW.GLFW_KEY_8),
    HOTBAR_SLOT_9(GLFW.GLFW_KEY_9),
    DESTROY(GLFW.GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON),
    USE(GLFW.GLFW_MOUSE_BUTTON_RIGHT | Input.IS_MOUSE_BUTTON),
    PICK_BLOCK(GLFW.GLFW_MOUSE_BUTTON_MIDDLE | Input.IS_MOUSE_BUTTON),
    INVENTORY(GLFW.GLFW_KEY_E),
    DEBUG_MENU(GLFW.GLFW_KEY_F3),
    NO_CLIP(GLFW.GLFW_KEY_P),
    ZOOM(GLFW.GLFW_KEY_V),
    INCREASE_BREAK_PLACE_SIZE(GLFW.GLFW_KEY_UP),
    DECREASE_BREAK_PLACE_SIZE(GLFW.GLFW_KEY_DOWN),
    DROP(GLFW.GLFW_KEY_Q),
    OPEN_CHAT(GLFW.GLFW_KEY_T),
    RESIZE_WINDOW(GLFW.GLFW_KEY_F11),
    RELOAD_ASSETS(GLFW.GLFW_KEY_F10),
    RELOAD_SETTINGS(GLFW.GLFW_KEY_F9),
    RELOAD_LANGUAGE(GLFW.GLFW_KEY_F8),
    RELOAD_FONT(GLFW.GLFW_KEY_F7),
    RELOAD_MATERIALS(GLFW.GLFW_KEY_F6),
    GET_CHUNK_REBUILD_PLACEABLE(GLFW.GLFW_KEY_F5),
    SKIP_COMPUTING_VISIBILITY(GLFW.GLFW_KEY_G);

    public static void setIfPresent(String name, String value) {
        try {
            valueOf(name).setValue(Integer.parseInt(value));
        } catch (IllegalArgumentException ignore) {

        }
    }

    KeySetting(int defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    void setValue(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public int defaultValue() {
        return defaultValue;
    }

    private final int defaultValue;
    private int value;
}
