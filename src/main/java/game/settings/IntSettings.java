package game.settings;

import core.settings.IntSetting;

public enum IntSettings implements IntSetting {
    FOV(1, 175, 90),
    REACH(0, 500, 5),
    BREAK_PLACE_INTERVALL(1, 20, 5),
    INVENTORY_ITEMS_PER_ROW(1, 64, 8),
    MAX_CHAT_MESSAGE_COUNT(10, 1000, 100),
    AMBIENT_OCCLUSION_SAMPLES(0, 64, 64),
    OCCLUDERS_OCCLUDEES_LOD(0, 30, 0);

    IntSettings(int min, int max, int defaultValue) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public Integer valueFromFraction(float fraction) {
        return Math.round(min + fraction * (max - min));
    }

    @Override
    public float fractionFromValue(Integer value) {
        return (float) (value - min) / (max - min);
    }

    @Override
    public int defaultValue() {
        return defaultValue;
    }

    private final int min, max, defaultValue;
    private int value;
}
