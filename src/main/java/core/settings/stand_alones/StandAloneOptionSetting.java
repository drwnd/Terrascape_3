package core.settings.stand_alones;

import core.settings.OptionSetting;
import core.settings.optionSettings.Option;

import static org.lwjgl.glfw.GLFW.*;

public final class StandAloneOptionSetting implements OptionSetting {

    public StandAloneOptionSetting(Option defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
        this.keybind = defaultKeybind;
    }

    StandAloneOptionSetting(Option defaultValue, int defaultKeybind) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = defaultKeybind;
        this.keybind = defaultKeybind;
    }

    @Override
    public void setValue(Option value) {
        this.value = value;
    }

    @Override
    public Option value() {
        return value;
    }

    @Override
    public Option defaultValue() {
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

    private Option value;
    private final Option defaultValue;

    private final int defaultKeybind;
    private int keybind;
}
