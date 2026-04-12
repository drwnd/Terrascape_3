package game.player.interaction;

import core.renderables.OptionToggle;
import core.renderables.Toggle;
import core.renderables.UiButton;
import core.settings.*;
import core.settings.stand_alones.StandAloneFloatSetting;
import core.settings.stand_alones.StandAloneIntSetting;
import core.settings.stand_alones.StandAloneOptionSetting;
import core.settings.stand_alones.StandAloneToggleSetting;
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

    public ShapeSetting copy() {
        Setting setting = switch (this.setting) {
            case IntSetting intSetting -> new StandAloneIntSetting(0, 0, intSetting.value());
            case FloatSetting floatSetting -> new StandAloneFloatSetting(0, 0, floatSetting.value());
            case ToggleSetting toggleSetting -> new StandAloneToggleSetting(toggleSetting.value());
            case OptionSetting optionSetting -> new StandAloneOptionSetting(optionSetting.value());
            case null, default -> throw new IllegalStateException("Unexpected value: " + this.setting);
        };
        return new ShapeSetting(setting, name, shaderName);
    }
}
