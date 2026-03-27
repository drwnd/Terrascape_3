package game.settings;

import core.settings.OptionSetting;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.Option;
import core.settings.optionSettings.Visibility;
import game.player.interaction.PlaceMode;

public enum OptionSettings implements OptionSetting {
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
    RENDERED_MODELS_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    VELOCITY_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    CHUNK_MEMORY_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    TOTAL_MEMORY_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),
    BUFFER_STORAGE_VISIBILITY(Visibility.WHEN_SCREEN_OPEN),

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
    RENDERED_MODELS_COLOR(ColorOption.RED),
    VELOCITY_COLOR(ColorOption.WHITE),
    CHUNK_MEMORY_COLOR(ColorOption.RED),
    TOTAL_MEMORY_COLOR(ColorOption.RED),
    BUFFER_STORAGE_COLOR(ColorOption.ORANGE),

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
