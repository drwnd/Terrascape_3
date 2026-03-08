package game.settings;

import core.settings.FloatSetting;
import core.utils.MathUtils;

public enum FloatSettings implements FloatSetting {
    FOV(1.0F, 175.0F, 90.0F),
    FOOTSTEPS_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    PLACE_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    DIG_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    INVENTORY_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    CROSSHAIR_SIZE(0.0F, 0.2F, 0.045454547F, 0.001F),
    HOTBAR_SIZE(0.0F, 0.2F, 0.05F, 0.001F),
    HOTBAR_INDICATOR_SCALER(0.0F, 3.0F, 1.2F, 0.01F),
    PAUSE_MENU_BACKGROUND_BLUR(0.0F, 10.0F, 0.0F, 0.1F),
    INVENTORY_ITEM_SIZE(0.01F, 0.1F, 0.02F, 0.001F),
    INVENTORY_ITEM_SCALING(1, 2, 1.2F, 0.01F),
    TIME_SPEED(0.0F, 0.001F, 0.00008333F, 0.000001F),
    DOWNWARD_SUN_DIRECTION(-1.0F, 1.0F, 0.3F),
    NIGHT_BRIGHTNESS(0.05F, 1.0F, 0.2F, 0.01F),
    CHAT_MESSAGE_DURATION(1.0F, 30.0F, 5.0F, 0.1F);

    FloatSettings(float min, float max, float defaultValue, float accuracy) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.accuracy = accuracy;
    }

    FloatSettings(float min, float max, float defaultValue) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.accuracy = 0.00001F;
    }

    @Override
    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public float value() {
        return value;
    }

    @Override
    public Float valueFromFraction(float fraction) {
        float unroundedValue = min + fraction * (max - min);
        float roundingOffset = MathUtils.absMin(-(unroundedValue % accuracy), accuracy - unroundedValue % accuracy);
        return unroundedValue + roundingOffset;
    }

    @Override
    public float fractionFromValue(Float value) {
        return (value - min) / (max - min);
    }

    @Override
    public float defaultValue() {
        return defaultValue;
    }

    private final float min, max, defaultValue, accuracy;
    private float value;
}
