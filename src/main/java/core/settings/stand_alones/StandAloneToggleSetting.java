package core.settings.stand_alones;

import core.settings.ToggleSetting;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public final class StandAloneToggleSetting implements ToggleSetting {

    public StandAloneToggleSetting(boolean defaultValue, int defaultKeybind) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = defaultKeybind;
        this.keybind = defaultKeybind;
    }

    public StandAloneToggleSetting(boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
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
