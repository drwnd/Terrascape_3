package core.settings;

import core.utils.MathUtils;

public enum CoreFloatSettings implements FloatSetting {
    GUI_SIZE(0.25F, 1.0F, 1.0F, 0.01F),
    SENSITIVITY(0.0F, 1.0F, 0.14612676056338028F),
    TEXT_SIZE(0.5F, 3.0F, 1.0F, 0.01F),
    MASTER_AUDIO(0.0F, 10.0F, 0.5F, 0.01F),
    UI_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    RIM_THICKNESS(0.0F, 0.1F, 0.015625F);

    CoreFloatSettings(float min, float max, float defaultValue, float accuracy) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.accuracy = accuracy;
    }

    CoreFloatSettings(float min, float max, float defaultValue) {
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
