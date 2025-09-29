package core.settings;

import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.FontOption;
import core.settings.optionSettings.Option;
import core.settings.optionSettings.Visibility;

public enum OptionSetting {

    WORLD_NAME_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    WORLD_TICK_AND_TIME_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    FPS_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    POSITION_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    CHUNK_POSITION_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    DIRECTION_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    ROTATION_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    SEED_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    CHUNK_STATUS_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    CHUNK_IDENTIFIERS_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    TARGET_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),

    WORLD_NAME_COLOR(ColorOption.LIGHT_GRAY),
    WORLD_TICK_AND_TIME_COLOR(ColorOption.LIGHT_GRAY),
    FPS_COLOR(ColorOption.RED),
    POSITION_COLOR(ColorOption.WHITE),
    CHUNK_POSITION_COLOR(ColorOption.WHITE),
    DIRECTION_COLOR(ColorOption.WHITE),
    ROTATION_COLOR(ColorOption.GRAY),
    SEED_COLOR(ColorOption.GRAY),
    CHUNK_STATUS_COLOR(ColorOption.GREEN),
    CHUNK_IDENTIFIERS_COLOR(ColorOption.GREEN),
    TARGET_COLOR(ColorOption.BLUE),

    FONT(new FontOption("Default"));

    public static void setIfPresent(String name, String value) {
        try {
            OptionSetting setting = valueOf(name);

            Option savedValue = setting.defaultValue.value(value);

            if (savedValue != null) setting.setValue(savedValue);
        } catch (IllegalArgumentException ignore) {

        }
    }

    OptionSetting(Option defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    void setValue(Option value) {
        this.value = value;
    }

    public Option value() {
        return value;
    }

    public Option defaultValue() {
        return defaultValue;
    }

    private Option value;
    private final Option defaultValue;
}
