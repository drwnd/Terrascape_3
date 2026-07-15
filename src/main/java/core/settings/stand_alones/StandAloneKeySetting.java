package core.settings.stand_alones;

import core.settings.KeySetting;

public final class StandAloneKeySetting implements KeySetting {

    public StandAloneKeySetting(int defaultValue) {
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

    @Override
    public String translationFileName() {
        throw new UnsupportedOperationException("Standalone Settings aren't Translatable");
    }

    @Override
    public int ordinal() {
        throw new UnsupportedOperationException("Standalone Settings aren't Translatable");
    }

    private final int defaultKeybind;
    private int keybind;
}
