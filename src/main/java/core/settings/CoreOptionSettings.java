package core.settings;

import core.settings.optionSettings.*;
import core.language.Language;

import static org.lwjgl.glfw.GLFW.*;

public enum CoreOptionSettings implements OptionSetting {
    FONT(new FontOption("Default")),
    LANGUAGE(new Language("English")),
    TEXTURE_PACK(new TexturePack("Default")),
    LOG_MESSAGES(LogMessages.NONE);

    CoreOptionSettings(Option defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
        this.keybind = GLFW_KEY_UNKNOWN;
    }

    CoreOptionSettings(Option defaultValue, int defaultKeybind) {
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
