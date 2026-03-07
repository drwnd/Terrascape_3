package game.settings;

import core.settings.KeySetting;

public enum KeySettings implements KeySetting {
    ;

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
