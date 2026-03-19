package core.settings;

import static org.lwjgl.glfw.GLFW.*;

public enum CoreKeySettings implements KeySetting {
    RESIZE_WINDOW(GLFW_KEY_F11),
    RELOAD_ASSETS(GLFW_KEY_F10),
    RELOAD_SETTINGS(GLFW_KEY_F9),
    RELOAD_FONT(GLFW_KEY_F7);

    CoreKeySettings(int defaultValue) {
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
