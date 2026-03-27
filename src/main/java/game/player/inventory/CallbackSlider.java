package game.player.inventory;

import core.renderables.Slider;
import core.settings.NumberSetting;
import core.utils.StringGetter;
import org.joml.Vector2f;

import java.util.Objects;

public final class CallbackSlider<T extends Number> extends Slider<T> {

    public CallbackSlider(Vector2f sizeToParent, Vector2f offsetToParent, NumberSetting<T> setting, StringGetter settingName, boolean updateImmediately) {
        super(sizeToParent, offsetToParent, setting, settingName, updateImmediately);
    }

    public void setSlidingCallback(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void setValue(T value) {
        if (callback != null && !Objects.equals(getValue(), value)) callback.run();
        super.setValue(value);
    }

    private Runnable callback;
}
