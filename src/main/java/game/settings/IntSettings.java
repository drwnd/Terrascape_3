package game.settings;

import core.settings.IntSetting;

import static game.utils.Constants.CHUNK_SIZE_BITS;

public enum IntSettings implements IntSetting {
    REACH(0, 500, 5),
    BREAK_PLACE_INTERVALL(1, 20, 5),
    MAX_CHAT_MESSAGE_COUNT(10, 1000, 100),
    AMBIENT_OCCLUSION_SAMPLES(0, 64, 64),
    OCCLUDERS_OCCLUDEES_LOD(0, 30, 0),
    BREAK_PLACE_SIZE(0, CHUNK_SIZE_BITS + 2, 4),
    BREAK_PLACE_ALIGN(0, CHUNK_SIZE_BITS + 2, 4);

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
