package settings;

public enum FloatSetting {
    FOV(10.0f, 175.0f, 90.0f, 1.0f),
    GUI_SIZE(0.25f, 1.0f, 1.0f, 0.01f),
    SENSITIVITY(0.0f, 1.0f, 0.14612676056338028f),
    REACH(0.0f, 500.0f, 5.0f, 1.0f),
    TEXT_SIZE(0.5f, 3.0f, 1.0f, 0.01f),
    MASTER_AUDIO(0.0f, 10.0f, 0.5f, 0.01f),
    FOOTSTEPS_AUDIO(0.0f, 5.0f, 1.0f, 0.01f),
    PLACE_AUDIO(0.0f, 5.0f, 1.0f, 0.01f),
    DIG_AUDIO(0.0f, 5.0f, 1.0f, 0.01f),
    INVENTORY_AUDIO(0.0f, 5.0f, 1.0f, 0.01f),
    MISCELLANEOUS_AUDIO(0.0f, 5.0f, 1.0f, 0.01f),
    RIM_THICKNESS(0.0f, 0.1f, 0.015625f),
    CROSSHAIR_SIZE(0.0f, 0.2f, 0.045454547f, 0.001f),
    HOTBAR_SIZE(0.0f, 0.2f, 0.05f, 0.001f),
    BREAK_PLACE_INTERVALL(0, 20, 5, 1),
    HOTBAR_INDICATOR_SCALER(0.0f, 3.0f, 1.2f, 0.01f),
    PAUSE_MENU_BACKGROUND_BLUR(0.0f, 10.0f, 0.0f, 0.1f);

    public static void setIfPresent(String name, String value) {
        try {
            valueOf(name).setValue(Float.parseFloat(value));
        } catch (IllegalArgumentException ignore) {

        }
    }

    FloatSetting(float min, float max, float defaultValue, float accuracy) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.accuracy = accuracy;
    }

    FloatSetting(float min, float max, float defaultValue) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.accuracy = 0.00001f;
    }

    void setValue(float value) {
        this.value = value;
    }

    public float value() {
        return value;
    }

    public float valueFronFraction(float fraction) {
        float unroundedValue = min + fraction * (max - min);
        float roundingOffset = absMin(-(unroundedValue % accuracy), accuracy - unroundedValue % accuracy);
        return unroundedValue + roundingOffset;
    }

    public float fractionFromValue(float value) {
        return (value - min) / (max - min);
    }

    public float defaultValue() {
        return defaultValue;
    }

    private static float absMin(float a, float b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }

    private final float min, max, defaultValue, accuracy;
    private float value;
}
