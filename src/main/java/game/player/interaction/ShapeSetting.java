package game.player.interaction;

import core.renderables.OptionToggle;
import core.renderables.Toggle;
import core.renderables.UiButton;
import core.settings.*;
import core.utils.StringGetter;

import game.player.inventory.CallbackSlider;

import org.joml.Vector2f;

import java.util.Objects;

public record ShapeSetting(Setting setting, StringGetter name, String shaderName) {

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

    public UiButton getSettingButton() {
        Vector2f zero = new Vector2f();
        return switch (setting) {
            case NumberSetting<?> numberSetting -> new CallbackSlider<>(zero, zero, numberSetting, name, true);
            case ToggleSetting toggleSetting -> new Toggle(zero, zero, toggleSetting, name, true);
            case OptionSetting optionSetting -> new OptionToggle(zero, zero, optionSetting, name, true);
            case null, default -> throw new IllegalStateException("Unexpected value: " + setting);
        };
    }
}
