package game.settings;

import core.settings.OptionSetting;
import core.settings.optionSettings.Option;
import game.player.interaction.PlaceMode;
import game.player.rendering.RenderingOptimizer;

import static org.lwjgl.glfw.GLFW.*;

public enum OptionSettings implements OptionSetting {
    PLACE_MODE(PlaceMode.REPLACE),
    OCCLUSION_CULLING(RenderingOptimizer.OcclusionCullingOptions.NORMAL);

    OptionSettings(Option defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
        this.keybind = defaultKeybind;
    }

    OptionSettings(Option defaultValue, int defaultKeybind) {
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
