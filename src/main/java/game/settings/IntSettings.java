package game.settings;

import core.settings.IntSetting;

import game.server.Game;

import static game.utils.Constants.CHUNK_SIZE_BITS;

public enum IntSettings implements IntSetting {
    MAX_CHAT_MESSAGE_COUNT(10, 1000, 100),
    AMBIENT_OCCLUSION_SAMPLES(0, 51, 51),
    BREAK_PARTICLE_STEP_LENGTH(1, 16, 2),
    PLACE_PARTICLE_STEP_LENGTH(1, 16, 1),
    RENDER_DISTANCE(2, 16, 6, Game::updateRenderDistance),
    LOD_COUNT(1, 20, 10, Game::updateLodCount),

    OCCLUDERS_OCCLUDEES_LOD(0, LOD_COUNT.max - 1, 0),
    BREAK_PLACE_SIZE(0, CHUNK_SIZE_BITS + 2, 4),
    BREAK_PLACE_ALIGN(0, CHUNK_SIZE_BITS + 2, 4),
    REACH(0, 500, 112),
    BREAK_PLACE_INTERVALL(1, 20, 4);

    /**
     * Constructs an integer setting with the specified range and default value.
     *
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param defaultValue the default value
     */
    IntSettings(int min, int max, int defaultValue) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.callback = null;
    }

    /**
     * Constructs an integer setting with the specified range, default value, and change callback.
     *
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param defaultValue the default value
     * @param callback the callback to run when the value changes
     */
    IntSettings(int min, int max, int defaultValue, ChangeCallback callback) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.callback = callback;
    }

    /**
     * Sets the value of this setting and triggers the callback if the value changed.
     *
     * @param value the new setting value
     */
    @Override
    public void setValue(int value) {
        int oldValue = this.value;
        this.value = value;
        if (oldValue != value && callback != null) callback.run(oldValue);
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public int min() {
        return min;
    }

    @Override
    public int max() {
        return max;
    }

    /**
     * Calculates the setting value from a normalized fraction (0 to 1).
     *
     * @param fraction the normalized fraction
     * @return the calculated setting value
     */
    @Override
    public Integer valueFromFraction(float fraction) {
        return Math.round(min + fraction * (max - min));
    }

    /**
     * Calculates the normalized fraction (0 to 1) from a setting value.
     *
     * @param value the setting value
     * @return the calculated normalized fraction
     */
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
    private final ChangeCallback callback;

    @Override
    public String translationFileName() {
        return "intSettings";
    }

    public interface ChangeCallback {
        void run(int oldValue);
    }
}
