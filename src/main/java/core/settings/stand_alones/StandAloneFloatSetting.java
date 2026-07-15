package core.settings.stand_alones;

import core.settings.FloatSetting;
import core.utils.MathUtils;

public final class StandAloneFloatSetting implements FloatSetting {

    public StandAloneFloatSetting(float min, float max, float defaultValue, float accuracy) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.accuracy = accuracy;
    }

    public StandAloneFloatSetting(float min, float max, float defaultValue) {
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
    public float min() {
        return min;
    }

    @Override
    public float max() {
        return max;
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

    @Override
    public String translationFileName() {
        throw new UnsupportedOperationException("Standalone Settings aren't Translatable");
    }

    @Override
    public int ordinal() {
        throw new UnsupportedOperationException("Standalone Settings aren't Translatable");
    }

    private final float min, max, defaultValue, accuracy;
    private float value;
}
