package game.settings;

import core.settings.ToggleSetting;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public enum ToggleSettings implements ToggleSetting {
    ;

    ToggleSettings(boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
        this.keybind = defaultKeybind;
    }

    ToggleSettings(boolean defaultValue, int defaultKeybind) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = defaultKeybind;
        this.keybind = defaultKeybind;
    }

    @Override
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean value() {
        return value;
    }

    @Override
    public boolean defaultValue() {
        return defaultValue;
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

    private final boolean defaultValue;
    private boolean value;

    private final int defaultKeybind;
    private int keybind;
}
