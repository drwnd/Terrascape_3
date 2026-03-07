package core.settings;

import core.utils.MathUtils;

public enum CoreFloatSettings implements FloatSetting {
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
    NIGHT_BRIGHTNESS(0.05F, 1.0F, 0.2F, 0.01F),
    CHAT_MESSAGE_DURATION(1.0F, 30.0F, 5.0F, 0.1F),
    MAX_CHAT_MESSAGE_COUNT(10, 1000, 100, 1),
    AMBIENT_OCCLUSION_SAMPLES(0, 64, 64, 1),
    OCCLUDERS_OCCLUDEES_LOD(0, 30, 0, 1);

    public static void setIfPresent(String name, String value) {
        try {
            valueOf(name).value = Float.parseFloat(value);
        } catch (IllegalArgumentException ignore) {

        }
    }

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
    public float valueFromFraction(float fraction) {
        float unroundedValue = min + fraction * (max - min);
        float roundingOffset = MathUtils.absMin(-(unroundedValue % accuracy), accuracy - unroundedValue % accuracy);
        return unroundedValue + roundingOffset;
    }

    @Override
    public float fractionFromValue(float value) {
        return (value - min) / (max - min);
    }

    @Override
    public float defaultValue() {
        return defaultValue;
    }

    private final float min, max, defaultValue, accuracy;
    private float value;
}
