package core.settings;

import core.settings.optionSettings.*;
import core.languages.Language;

public enum CoreOptionSettings implements OptionSetting {
    FONT(new FontOption("Default")),
    LANGUAGE(new Language("English")),
    TEXTURE_PACK(new TexturePack("Default"));

    CoreOptionSettings(Option defaultValue) {
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
