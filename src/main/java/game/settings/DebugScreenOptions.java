package game.settings;

import core.settings.OptionSetting;
import core.settings.Setting;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.Option;
import core.settings.optionSettings.Visibility;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public enum DebugScreenOptions {

    WORLD_NAME(ColorOption.LIGHT_GRAY),
    WORLD_TICK_AND_TIME(ColorOption.LIGHT_GRAY),
    FPS(ColorOption.RED),
    POSITION(ColorOption.WHITE),
    CHUNK_POSITION(ColorOption.WHITE),
    DIRECTION(ColorOption.WHITE),
    ROTATION(ColorOption.GRAY),
    SEED(ColorOption.GRAY),
    CHUNK_STATUS(ColorOption.GREEN),
    CHUNK_IDENTIFIERS(ColorOption.GREEN),
    TARGET(ColorOption.BLUE),
    RENDERED_MODELS(ColorOption.RED),
    VELOCITY(ColorOption.WHITE),
    CHUNK_MEMORY(ColorOption.RED),
    TOTAL_MEMORY(ColorOption.RED),
    BUFFER_STORAGE(ColorOption.ORANGE),
    GENERATION_DATA(ColorOption.WHITE),
    BIOME(ColorOption.GREEN),
    RESULTING_HEIGHT(ColorOption.GREEN),
    CONCURRENTLY_PLAYED_SOUNDS(ColorOption.WHITE);

    /**
     * Constructs a new debug screen option with the specified default color.
     *
     * @param color the default color for this debug option
     */
    DebugScreenOptions(Option color) {
        this.visibility = new DebugOption(Visibility.WHEN_SCREEN_OPEN, this.name() + "_VISIBILITY");
        this.color = new DebugOption(color, this.name() + "_COLOR");
    }


    /**
     * Returns a list of all visibility settings for the debug screen options.
     *
     * @return a list of visibility settings
     */
    public static List<Setting> getVisibilitySettings() {
        DebugScreenOptions[] values = DebugScreenOptions.values();
        ArrayList<Setting> settings = new ArrayList<>(values.length);
        for (DebugScreenOptions options : values) settings.add(options.visibility);
        return settings;
    }

    /**
     * Returns a list of all color settings for the debug screen options.
     *
     * @return a list of color settings
     */
    public static List<Setting> getColorSettings() {
        DebugScreenOptions[] values = DebugScreenOptions.values();
        ArrayList<Setting> settings = new ArrayList<>(values.length);
        for (DebugScreenOptions options : values) settings.add(options.color);
        return settings;
    }


    public OptionSetting getVisibility() {
        return visibility;
    }

    public OptionSetting getColor() {
        return color;
    }

    private final OptionSetting visibility, color;

    private static class DebugOption implements OptionSetting {

        /**
         * Creates a new debug option with the given default value and name.
         *
         * @param defaultValue the default value for this option
         * @param name the name of this option
         */
        private DebugOption(Option defaultValue, String name) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        @Override
        public String name() {
            return name;
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

        }

        @Override
        public int keybind() {
            return GLFW_KEY_UNKNOWN;
        }

        @Override
        public int defaultKeybind() {
            return GLFW_KEY_UNKNOWN;
        }

        @Override
        public String translationFileName() {
            throw new UnsupportedOperationException("DebugOptions aren't Translatable");
        }

        @Override
        public int ordinal() {
            throw new UnsupportedOperationException("DebugOptions aren't Translatable");
        }


        private final String name;
        private final Option defaultValue;
        private Option value;
    }
}
