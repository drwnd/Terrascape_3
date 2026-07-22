package game.player.interaction;

import core.renderables.OptionToggle;
import core.renderables.Toggle;
import core.renderables.UiButton;
import core.rendering_api.shaders.Shader;
import core.settings.*;
import core.utils.StringGetter;

import game.player.inventory.CallbackSlider;

import org.joml.Vector2f;

import java.util.Objects;

public record ShapeSetting(Setting setting, StringGetter name, String uniformName) {

    /**
     * Calculates the hash code for the setting based on its value.
     * @return the hash code of the setting's value
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(switch (setting) {
            case IntSetting intSetting -> intSetting.value();
            case FloatSetting floatSetting -> floatSetting.value();
            case ToggleSetting toggleSetting -> toggleSetting.value();
            case OptionSetting optionSetting -> optionSetting.value();
            case null, default -> throw new IllegalStateException("Unexpected value: " + setting);
        });
    }

    /**
     * Creates a UI button corresponding to the type of setting.
     * @return a {@link UiButton} for controlling this setting
     */
    public UiButton getSettingButton() {
        Vector2f zero = new Vector2f();
        return switch (setting) {
            case NumberSetting<?> numberSetting -> new CallbackSlider<>(zero, zero, numberSetting, name, true);
            case ToggleSetting toggleSetting -> new Toggle(zero, zero, toggleSetting, name, true);
            case OptionSetting optionSetting -> new OptionToggle(zero, zero, optionSetting, name, true);
            case null, default -> throw new IllegalStateException("Unexpected value: " + setting);
        };
    }

    /**
     * Sets the uniform value for this setting in the provided shader.
     * @param shader the shader to update
     */
    public void setUniform(Shader shader) {
        switch (setting) {
            case IntSetting intSetting -> shader.setUniform(uniformName, intSetting.value());
            case FloatSetting floatSetting -> shader.setUniform(uniformName, floatSetting.value());
            case ToggleSetting toggleSetting -> shader.setUniform(uniformName, toggleSetting.value());
            case OptionSetting optionSetting -> shader.setUniform(uniformName, optionSetting.value().ordinal());
            case null, default -> throw new IllegalStateException("Unexpected value: " + setting);
        }
    }
}
