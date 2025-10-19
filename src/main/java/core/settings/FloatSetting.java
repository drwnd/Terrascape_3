package core.settings;

public enum FloatSetting {
    FOV(1.0F, 175.0F, 90.0F, 1.0F),
    GUI_SIZE(0.25F, 1.0F, 1.0F, 0.01F),
    SENSITIVITY(0.0F, 1.0F, 0.14612676056338028F),
    REACH(0.0F, 500.0F, 5.0F, 1.0F),
    TEXT_SIZE(0.5F, 3.0F, 1.0F, 0.01F),
    MASTER_AUDIO(0.0F, 10.0F, 0.5F, 0.01F),
    FOOTSTEPS_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    PLACE_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    DIG_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    INVENTORY_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    MISCELLANEOUS_AUDIO(0.0F, 5.0F, 1.0F, 0.01F),
    RIM_THICKNESS(0.0F, 0.1F, 0.015625F),
    CROSSHAIR_SIZE(0.0F, 0.2F, 0.045454547F, 0.001F),
    HOTBAR_SIZE(0.0F, 0.2F, 0.05F, 0.001F),
    BREAK_PLACE_INTERVALL(1, 20, 5, 1),
    HOTBAR_INDICATOR_SCALER(0.0F, 3.0F, 1.2F, 0.01F),
    PAUSE_MENU_BACKGROUND_BLUR(0.0F, 10.0F, 0.0F, 0.1F),
    INVENTORY_ITEM_SIZE(0.01F, 0.1F, 0.02F, 0.001F),
    INVENTORY_ITEMS_PER_ROW(1, 64, 8, 1),
    INVENTORY_ITEM_SCALING(1, 2, 1.2F, 0.01F),
    TIME_SPEED(0.0F, 0.001F, 0.00008333F, 0.000001F),
    DOWNWARD_SUN_DIRECTION(-1.0F, 1.0F, 0.3F),
    NIGHT_BRIGHTNESS(0.05F, 1.0F, 0.2F, 0.01F);

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
