package game.player.inventory;

import core.renderables.Slider;
import core.settings.NumberSetting;
import core.utils.StringGetter;
import org.joml.Vector2f;
import org.joml.Vector2i;

public final class CallbackSlider<T extends Number> extends Slider<T> {

    public CallbackSlider(Vector2f sizeToParent, Vector2f offsetToParent, NumberSetting<T> setting, StringGetter settingName, boolean updateImmediately) {
        super(sizeToParent, offsetToParent, setting, settingName, updateImmediately);
    }

    public void setSlidingCallback(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void dragOver(Vector2i pixelCoordinate) {
        super.dragOver(pixelCoordinate);
        if (callback != null) callback.run();
    }

    private Runnable callback;
}
