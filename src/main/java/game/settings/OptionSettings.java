package game.settings;

import core.settings.OptionSetting;
import core.settings.optionSettings.Option;
import game.player.interaction.PlaceMode;

public enum OptionSettings implements OptionSetting {
    PLACE_MODE(PlaceMode.REPLACE);

    OptionSettings(Option defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
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

    private Option value;
    private final Option defaultValue;
}
