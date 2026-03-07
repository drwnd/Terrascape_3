package game.settings;

import core.settings.OptionSetting;
import core.settings.optionSettings.Option;

public enum OptionSettings implements OptionSetting {
    ;

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
